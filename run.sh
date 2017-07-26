#!/usr/bin/env bash

MBdate=`date -d '-25 day' +%Y-%m-%d | tr -d '\n'`
MBdate=(${MBdate//-/ })
Ryear=${MBdate[0]}
Rmonth=${MBdate[1]}


#Ryear=2017
#Rmonth=05

NEO_HOST=10.32.117.167
NEO_USER="neo_ro"
NEO_PWD="neo_ro123"

REPORT_DB_HOST=10.33.145.239
REPOT_DB_USER="gjx_core"
REPORT_DB_PWD="gjx123"

START_DATE="${Ryear}-${Rmonth}-01 00:00:00"



`mysqldump --single-transaction -h ${REPORT_DB_HOST} -u ${REPOT_DB_USER} -p${REPORT_DB_PWD} dp_reports_db dp_daily_banner_metrics --opt --where="year=${Ryear} AND month=${Rmonth}" > /tmp/dp_daily_banner_metrics.sql`
`mysql -u root -proot\@123\# neo < /tmp/dp_daily_banner_metrics.sql`


`mysql -u root -proot\@123\# neo -e"truncate table neo.banner"`
`mysqldump --single-transaction --no-create-info -h ${NEO_HOST} -u ${NEO_USER} -p${NEO_PWD} neo banner > /tmp/banner.sql`
`mysql -u root -proot\@123\# neo -e"source /tmp/banner.sql"`

`mysql -u root -proot\@123\# neo -e"truncate table neo.campaign"`
`mysqldump --single-transaction --no-create-info -h ${NEO_HOST} -u ${NEO_USER} -p${NEO_PWD} neo campaign > /tmp/campaign.sql`
`mysql -u root -proot\@123\# neo -e "source /tmp/campaign.sql"`


`mysqldump --single-transaction  -h ${NEO_HOST} -u ${NEO_USER} -p${NEO_PWD} neo release_order > /tmp/release_order.sql`
`mysql -u root -proot\@123\# neo < /tmp/release_order.sql`


filename="billing_${Ryear}_${Rmonth}.xls";

/usr/lib/jvm/jdk-8-oracle-x64/bin/java -jar /home/rahul.sachan/ads-demand-reporting/target/demand_platform-1.0-SNAPSHOT.jar ${Ryear} ${Rmonth} ""
#/usr/lib/jvm/j2sdk1.8-oracle/bin/java -jar target/demand_platform-1.0-SNAPSHOT.jar ${year} ${month}

echo "$filename"



address="rahul.sachan\@flipkart.com";
address="1P-Ad-Ops\@flipkart.com,fsa\@flipkart.com,rahul.sachan\@flipkart.com,mukund.kedia\@flipkart.com,manoj.kulatharayil\@flipkart.com,navendu.sharma\@flipkart.com,santosh.pawar\@flipkart.com";
echo "PFA of Brand Campaign billing report for ${Ryear}-${Rmonth}" | mutt -a "/tmp/$filename" -s "Brand Campaign ${Ryear}-${Rmonth} billing report" -- ${address};