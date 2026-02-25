## Creds for CF cli
cf login -a https://api.cf.us10-001.hana.ondemand.com -u aliaksandr.tsviatkou@sap.com -p MilaNika092025! -o 0658761dtrial


## Get flag
./cf-services.sh --get-flag use-destination-smtp

## Mailtrap
https://mailtrap.io/inboxes/4393830/messages/5345240659


mvn clean package -P cloud -DskipTests
mbt build && cf deploy mta_archives/learning-management-system_0.1.0.mtar