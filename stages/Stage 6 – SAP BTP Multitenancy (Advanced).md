# Stage 6 – SAP BTP Multitenancy — Advanced

# Functional requirement

* The system must identify the current tenant from the `JWT` and use this information to automatically provision a dedicated database schema upon **subscription** and de-provision it on **unsubscription**. 
* It will use this **per-tenant schema** to store and retrieve data through a dedicated connection pool. 
* Furthermore, the application must be able to adapt its external API calls, like those to the `Destination` service, by first checking the **subscriber's configuration** before falling back to the **provider's**.

## Steps

1. **[AUTH]** Implement request filter to set current tenant info (**tenantId**, **subdomain**) from `JWT` into request context (**tenantId** = JWT.zid, **subdomain** = JWT.subdomain)
2. **[CF]** Implement logic for new database schema creation/deletion on tenant subscription/unsubscription locally
3. **[CF]** Implement logic for new HANA schema creation/deletion via **Service Manager** API on tenant subscription/unsubscription in cloud
4. **[DB]** Adapt Liquibase schema migration to multitenant mode
5. **[DB]** Configure a connection to the required schema using Hibernate
6. **[DB]** Implement data source with connection pools per tenant
7. **[DESTINATION]** Adjust logic with Destination service integration to multitenant mode (i.e. try to find destination by name on subscriber subaccount level firstly, if not presented – on provider subaccount level)  
8. **[DESTINATION]** Implement endpoint **/dependencies** that returns Destination service xsappname during subscription  
9. **[DESTINATION]** Create Destination with SMTP server credentials on the `Subscriber subaccount` level with the same name as at `Provider subaccount` level

## **Achievements**

#### [CF]
1. Multitenancy on DB level understanding  
2. HTTP request filters  
3. Request context  
4. Service Manager  
5. Destination Service multitenancy

### [JAVA]
1. ThreadLocal variables  

#### [DB]
1. DB connection pool  
2. Hibernate multitenancy  
3. Liquibase manual configuration  
