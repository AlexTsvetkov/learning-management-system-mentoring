# Stage 5 – SAP BTP Multitenancy - Basics

# Functional requirement

* The system must be configured with an `Approuter` to handle all incoming requests and provide a centralized entry point. 
It needs to expose a secure endpoint, accessible only to users with the "ADMIN" role, that returns the token URL, client ID, and client secret of the bound XSUAA service. 
* Furthermore, the application must be able to handle subscription and unsubscription requests from the SaaS Provisioning Service, dynamically returning a tenant-specific Approuter URL upon subscription. 
* All security roles and scopes must be defined in the `xs-security.json` file, and the application must be registered in a marketplace using the `saas-provisioning.json` file to enable subscription by external tenants.

## Steps

1. **[CF]** Create and configure **Approuter**  
2. **[CF]** Implement API that returns `tokenUrl`, `clientId`, `clientSecret` of bound XSUAA service instance (endpoint **/api/v1/application-info**) and allows access for an `ADMIN` role only  
3. **[CF]** Update Approuter’s **welcomFile** path to endpoint from Step 2  
4. **[CF]** Create **xs-security.json** file with role templates for **XSUAA**
5. **[CF]** Implement API to receive **subscription/unsubscription** requests from **SaaS Provisioning Service** that returns tenant-specific approuter URL + endpoint from Step 2 on subscription (i.e. [https://{{tenant-subdomain}}-{{approuter-name}}](https://host/api/v1/application-info))
6. **[CF]** Create **saas-provisioning.json** file with application details for **SaaS Provisioning Service** to register application in marketplace  
7. **[CF]** Create a subscriber subaccount and subscribe to the application

### **Achievements**

#### [CF]
1. SAP BTP Multitenancy understanding  
2. Provider and Subscriber subaccounts  
3. Approuter  
4. XSUAA roles creation  
5. SaaS Provisioning Service  
6. Application registration in Marketplace
