#!/usr/bin/env bash


REPORT_DB_HOST=10.33.145.239
REPOT_DB_USER="gjx_core"
REPORT_DB_PWD="gjx123"

MBdate=`date -d '2 hour ago' +%Y-%m-%d-%H-%M`

MBdate=(${MBdate//-/ })
year=${MBdate[0]}
month=${MBdate[1]}
day=${MBdate[2]}
hour=${MBdate[3]}

#year='2017'
#month='06'
#day='20'
#hour='10'
repType='hourly'

#data=`mysql --skip-column-names -h ${REPORT_DB_HOST} -u ${REPOT_DB_USER} -p${REPORT_DB_PWD} dp_reports_db -e"select count(*) from dp_third_party_reporting where DAY(cnv_time)=${day} and MONTH(cnv_time)=${month} and HOUR(cnv_time)=${hour}"`
#if [  "$data" = "0" ]
#then
#address="rahul.sachan\@flipkart.com,mukund.kedia\@flipkart.com,manoj.kulatharayil\@flipkart.com,ayan.d\@flipkart.com";
#echo "3P lead generation hourly report fail due to no data available for ${hour}" | mutt -s "3P lead generation report fail due to no data available" -- ${address};
#else
 /usr/lib/jvm/jdk-8-oracle-x64/bin/java -cp /home/rahul.sachan/ads-demand-reporting/target/demand_platform-1.0-SNAPSHOT.jar com.flipkart.ads.report.LeadGeneration hourly ${year} ${month} ${day} ${hour}
#fi
