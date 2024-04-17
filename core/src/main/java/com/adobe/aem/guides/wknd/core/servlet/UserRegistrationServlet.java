package com.adobe.aem.guides.wknd.core.servlet;

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

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(service = {Servlet.class}, property = {
        "sling.servlet.methods=GET",
        "sling.servlet.paths=/bin/user-registration"
}
)
public class UserRegistrationServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationServlet.class);
    private static final String SYSTEM_SUB_SERVICE = "wknd-sub-service";
    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        // Create the Map that specifies the SubService ID
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SYSTEM_SUB_SERVICE);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {

            String userName = request.getParameter("userName");

            //Response writer
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().write("Resource Resolver :" + resourceResolver.toString() + "User Name " + userName);

        } catch (LoginException | IOException e) {
            LOGGER.error("Login exception for service: {}", e.getMessage());
        }
    }
}

