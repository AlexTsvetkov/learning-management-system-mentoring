# Stage 4 – JPA Inheritance and i18n

# Functional requirement

* The system must handle two distinct types of lessons, "ClassroomLesson" and "VideoLesson", 
by creating new entities that share common fields from a parent "Lesson" class. 
* It must also support email localization by adding a "locale" field to the "Student" entity and using Mustache to create internationalized email templates for course notifications. 
* The system needs to provide proper data auditing by adding fields for creation and modification details to relevant entities. 
* Furthermore, all "GET all" API requests must support pagination, and the application must resolve any N+1 query issues to ensure efficient data retrieval. 
* Finally, the application must be deployable to the cloud using an MTA extension file.

## Steps

1. **[DB]** Add new entities `ClassroomLesson` and `VideoLesson` and keep common fields in `Lesson` class  
2. **[i18n]** Add “_locale_” field to `Student` for email message localization purpose  
3. **[i18n]** Add email message templates for notifications about course start in different languages using `Mustache`  
4. **[AUDIT]** Add audit fields (`created`, `createdBy`, `lastChanged`, `lastChangedBy`)  
5. **[DB]** Resolve N+1 issue  
6. **[WEB]** Add **pagination** for GET all requests  
7. **[TEST]** Split tests into two folders (unit and integration) based on type of test  
8. **[TEST]** Add `Surefire` maven plugin for unit tests  
9. **[TEST]** Add `Failsafe` maven plugin for integration tests  
10. **[TEST]** Add `Jacoco` maven plugin for test coverage reports generation (generate three binary (.exec) and HTML (.html) reports: for (1) unit and (2) integration tests and merged (3) results)  
11. **[CF]** Add `MTA extension` file and redeploy application with it

## New DB models: 

### ClassroomLesson

| Field     | Type    |
|-----------|---------|
| location  | String  |
| capacity  | Integer |

### VideoLesson

| Field    | Type   |
|----------|--------|
| url      | String |
| platform | String |


## **Achievements**

#### [DB]
1. JPA inheritance  
2. Audit fields  
3. @EntityGraph  

#### [i18n]
1. I18n  
2. Mustache  

#### [AUDIT]
1. SecurityContextHolder  
2. Pageable  
3. @EnableJpaAuditing  
4. @EntityListeners  

#### [TEST]
1. @Tag  
2. Surefire  
3. Failsafe  
4. Jacoco  

#### [CF]
1. MTA extension
