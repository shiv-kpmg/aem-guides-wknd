package com.adobe.aem.guides.wknd.core.servlet;

import com.google.gson.JsonObject;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.*;
import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = "wknd/components/user-registration",
        methods = {HttpConstants.METHOD_POST},
        selectors = "register",
        extensions = "json"
)
public class UserRegistrationServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationServlet.class);
    private static final String SYSTEM_SUB_SERVICE = "wknd-sub-service";
    private static final String CONTENT_AUTHORS = "content-authors";

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        //User creation status
        boolean creationFlag = true;

        // Create the Map that specifies the SubService ID
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SYSTEM_SUB_SERVICE);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {

            LOGGER.info("Resource Resolver in Registration Servlet {}", resourceResolver);

            //Getting session from resource resolver
            Session session = resourceResolver.adaptTo(Session.class);

            //Reading the parameter from servlet request
            String userData = request.getParameter("userData");

            //Split the string into key-value pairs
            String[] keyValuePairs = userData.split("&");

            //Create a map to store the extracted values
            Map<String, String> valuesMap = new HashMap<>();

            //Extract the values and store them in the map
            for (String pair : keyValuePairs) {
                String[] keyValue = pair.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                valuesMap.put(key, value);
            }

            String userFirstName = valuesMap.get("userFirstName");
            String userEmail = valuesMap.get("userEmail");
            String userId = valuesMap.get("userId");
            String userPassword = valuesMap.get("userPassword");

            //Checking if the User already exists
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            assert userManager != null;
            Authorizable authorizable = userManager.getAuthorizable(userId);

            if (authorizable != null) {
                // User exist set user creation flag as false
                creationFlag = false;
            } else {
                //Creation of a new user with userID
                User createdUser = userManager.createUser(userId, userPassword);

                //Setting the createdUser Profile Property
                assert session != null;
                ValueFactory valueFactory = session.getValueFactory();
                Value firstNameValue = valueFactory.createValue(userFirstName, PropertyType.STRING);
                createdUser.setProperty("./profile/givenName", firstNameValue);

                Value emailValue = valueFactory.createValue(userEmail, PropertyType.STRING);
                createdUser.setProperty("./profile/email", emailValue);

                LOGGER.info("User successfully created with ID : {}", createdUser.getID());

                //Addition of User to content-authors group
                Group group = (Group) userManager.getAuthorizable(CONTENT_AUTHORS);
                if (group != null) {
                    Authorizable authorizeUser = userManager.getAuthorizable(createdUser.getID());
                    assert authorizeUser != null;
                    group.addMember(authorizeUser);
                }

                //saving the session
                session.save();
            }

            //Response writer
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("creationFlag", creationFlag);
            jsonObject.addProperty("userId", userId);

            response.getWriter().write(String.valueOf(jsonObject));

        } catch (LoginException | IOException | RepositoryException e) {
            LOGGER.error("Login exception for service: {}", e.getMessage());
        }
    }
}

