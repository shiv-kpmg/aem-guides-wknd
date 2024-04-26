package com.adobe.aem.guides.wknd.core.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.*;
import javax.servlet.Servlet;

@Component(service = {Servlet.class}, property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/user-registration"})
public class UserRegistrationServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationServlet.class);
    private static final String SYSTEM_SUB_SERVICE = "wknd-sub-service";
    private static final String CONTENT_AUTHORS = "content-authors";
    boolean creationFlag = true;
    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        // Create the Map that specifies the SubService ID
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SYSTEM_SUB_SERVICE);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {

            LOGGER.info("Resource Resolver in Registration Servlet {}", resourceResolver);

            //Getting session from resource resolver
            Session session = resourceResolver.adaptTo(Session.class);

            //Reading the parameter from servlet request
            String userFirstName = request.getParameter("userFirstName");
            String userLastName = request.getParameter("userLastName");
            String userEmail = request.getParameter("userEmail");
            String userId = request.getParameter("userId");
            String userPassword = request.getParameter("userPassword");

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

                Value lastNameValue = valueFactory.createValue(userLastName, PropertyType.STRING);
                createdUser.setProperty("./profile/familyName", lastNameValue);

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
            response.getWriter().write("User created successfully with UserId :" + userId);

        } catch (LoginException | IOException | RepositoryException e) {
            LOGGER.error("Login exception for service: {}", e.getMessage());
        }
    }
}

