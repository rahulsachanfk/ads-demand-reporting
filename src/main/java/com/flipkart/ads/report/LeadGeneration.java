package com.flipkart.ads.report;

//apache poi imports


import com.flipkart.ad.dp.ConfigProvider;
import com.flipkart.ad.dp.Constant;
import com.sun.jersey.api.client.Client;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by rahul.sachan on 28/06/17.
 */

//ssh -f -N -L 7788:10.33.145.239:3306 10.32.246.118
//ssh -f -N -L 7766:10.32.117.167:3306 10.32.246.118
//ssh -f -N -L 7755:10.33.10.193:3306 10.32.246.118
public class LeadGeneration {

    public static void main(String[] args) throws IOException, SQLException, JSONException, ParseException {

        String repType = args[0];
        String year = args[1];
        String month = args[2];
        String day = args[3];
        String hour = args[4];

//        String repType = "daily";
//        String year = "2017";
//        String month = "07";
//        String day = "24";
//        String hour = "12";

        String date = day+"/"+month+"/"+year;
        Date date1= null;
        String dateAgo = null;
        try {
            date1 = strToDate(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Date daysAgo = new DateTime(date1).minusDays(15).toDate();
         dateAgo = dateToStr(daysAgo);
         date = dateToStr(date1);
        Client client = Client.create();
        Connection con = JDBCUtill.getJdbcConnection("jdbc:mysql://10.32.117.167:3306/neo", "neo_ro", "neo_ro123");
        Connection con1 = JDBCUtill.getJdbcConnection("jdbc:mysql://10.33.145.239:3306/dp_reports_db", "gjx_core", "gjx123");
        Connection con2 = JDBCUtill.getJdbcConnection("jdbc:mysql://10.33.10.193:3306/demand_report", "demand", "demand123");

//        Connection con = JDBCUtill.getJdbcConnection("jdbc:mysql://127.0.0.1:7766/neo", "neo_ro", "neo_ro123");
//        Connection con1 = JDBCUtill.getJdbcConnection("jdbc:mysql://127.0.0.1:7788/dp_reports_db", "gjx_core", "gjx123");
//        Connection con2 = JDBCUtill.getJdbcConnection("jdbc:mysql://127.0.0.1:7755/demand_report", "demand", "demand123");

        final String fields[] = {"CampName", "Conversion time", "Customer name", "Mobile number", "Email"};
        String mailingApi = "http://" + ConfigProvider.getInstance().get(Constant.MAIL_HOST, "") + "/v1/send/email/flipkart-promo";
        String stencilId = ConfigProvider.getInstance().get(Constant.MAIL_STENCILID, "");
        String XLXS_PWD = ConfigProvider.getInstance().get(Constant.XLXS_PWD, "");
        String bcc = ConfigProvider.getInstance().get(Constant.MAIL_BCC, "");
        String to = "rahul.sachan@flipkart.com";
        PreparedStatement pr;
        String RPTQUERY = null;
        String BRANDQUERY = null;
        String mailSub = "Lead generation Report";
        String mailBody = null;
        if (repType.equals("hourly")) {
            BRANDQUERY = "select * from advertiser_mailing_hourly where status=1";
        } else if (repType.equals("daily")) {
            BRANDQUERY = "select * from advertiser_mailing_daily where status=1";
        } else {
            System.out.println("Please pass correct parameter");
        }

        pr = con2.prepareStatement(BRANDQUERY);
        ResultSet rs = pr.executeQuery();
        while (rs.next()) {
            String brand = rs.getString("brand");
            String advName = rs.getString("advertiser");
            String mailIds = rs.getString("email");
            String[] Mails = mailIds.split(",");
            JSONArray ja = new JSONArray();
            for (String m: Mails) {
                JSONObject jo = new JSONObject();
                jo.put("name","");
                jo.put("address",m);
                ja.put(jo);
            }
            to = ja.toString();

            HashMap<String, String> cmpIdNameMap = new HashMap<String, String>();
            String cmpStr = "";
            int process = 0;
            String CMPQUERY = "select id,name from campaign where name like '"+ brand + "%' and (status='SERVICEABLE' or (status in('ABORTED','PAUSED','DELETED') and (updated_at +INTERVAL 2 DAY) > NOW() ))";
            //String CMPQUERY = "select id,name from campaign where name like '"+brand+"%'";
            pr = con.prepareStatement(CMPQUERY);
            ResultSet cmrs = pr.executeQuery();

            while (cmrs.next()) {
                process = 1;
                cmpIdNameMap.put(cmrs.getString("id"), cmrs.getString("name"));
                cmpStr = cmpStr + "'" + cmrs.getString("id") + "',";
            }
            cmpStr = cmpStr.substring(0, cmpStr.length() - 1);

            if (process == 1) {
                if (repType.equals("hourly")) {
                    mailBody = "Please find attached leads till " + year + "-" + month + "-" + day + ":" + hour;
                    RPTQUERY = "select * from dp_third_party_reporting where cmpid in (" + cmpStr + ")  and date(cnv_time) BETWEEN \'" + dateAgo + "\' and \'" + date+"\' order by cnv_time";
                } else if (repType.equals("daily")) {
                    mailBody = "Please find attached leads till " + year + "-" + month + "-" + day;
                    RPTQUERY = "select * from dp_third_party_reporting where cmpid in (" + cmpStr + ")  and  date(cnv_time) BETWEEN \'" + dateAgo + "\' and \'" + date+"\' order by cnv_time";
                }
                int hasData = 0;
                pr = con1.prepareStatement(RPTQUERY);
                ResultSet rptrs = pr.executeQuery();


                try (Workbook wb = new XSSFWorkbook()) {
                    StyleSheet css = new StyleSheet();

                    POIFSFileSystem fileSystem = new POIFSFileSystem();
                    Sheet sheet = wb.createSheet();
                    Row headerRow = sheet.createRow(0);
                    for (int i = 0; i < fields.length; i++) {
                        Cell fieldCell = headerRow.createCell(i);
                        fieldCell.setCellValue(fields[i]);
                        fieldCell.setCellStyle(css.getHeadingCss(wb));
                    }
                    int rownumber = 1;
                    while (rptrs.next()) {
                        Row r = sheet.createRow(rownumber++);
                        hasData = 1;
                        String cnv_formdata = rptrs.getString("cnv_formdata");
                        JSONObject jsonObj = new JSONObject(cnv_formdata);

                        Cell cell = r.createCell(0);
                        cell.setCellValue(cmpIdNameMap.get(rptrs.getString("cmpid")));

                        Cell cell1 = r.createCell(1);
                        cell1.setCellValue(rptrs.getString("cnv_time"));

                        Cell cell2 = r.createCell(2);
                        cell2.setCellValue(jsonObj.getString("customer_name"));

                        Cell cell3 = r.createCell(3);
                        cell3.setCellValue(jsonObj.getString("mobile_number"));

                        Cell cell4 = r.createCell(4);
                        cell4.setCellValue(jsonObj.getString("email"));
                    }
                    if (hasData == 1) {
                        EncryptionInfo info = new EncryptionInfo(EncryptionMode.standard);
                        Encryptor enc = info.getEncryptor();
                        enc.confirmPassword(XLXS_PWD);
                        OutputStream encryptedDS = enc.getDataStream(fileSystem);
                        wb.write(encryptedDS);
                        FileOutputStream fos = new FileOutputStream("/tmp/example.xlsx");
                        fileSystem.writeFilesystem(fos);
                        fos.close();
                        String base64 = DatatypeConverter.printBase64Binary(Files.readAllBytes(
                                Paths.get("/tmp/example.xlsx")));
                        mailSub = "Lead generation Report of " + brand;
                        String data = "{\n" +
                                "    \"sla\": \"H\",\n" +
                                "    \"channelInfo\": {\n" +
                                "                \"type\": \"EMAIL\",\n" +
                                "                \"appName\": \"flipkart\",\n" +
                                "                \"to\": "+to+",\n" +
                                "                \"cc\": [],\n" +
                                "                \"bcc\": [\n" +
                                "					{\n" +
                                "						\"name\": \"leadgen-mails\",\n" +
                                "						\"address\": \"" + bcc + "\"\n" +
                                "					}\n" +
                                "					],\n" +
                                "                \"from\": {\n" +
                                "                    \"name\": \"Flipkart.com\",\n" +
                                "                    \"address\": \"noreply@flipkart.com\"\n" +
                                "                },\n" +
                                "                \"replyTo\": null\n" +
                                "    },\n" +
                                "    \"channelDataModel\":{\n" +
                                "                \"type\": \"EMAIL\",\n" +
                                "                \"subject\": \"" + mailSub + "\",\n" +
                                "                \"html\": null,\n" +
                                "				\"message\":\"" + mailBody + "\",\n" +
                                "                \"text\": \"This is some dummy text modified.\"\n" +
                                "            },\n" +
                                "    \"channelData\":{\n" +
                                "      \"type\": \"EMAIL\",\n" +
                                "      \"attachments\": [\n" +
                                "        \n" +
                                "                    {\n" +
                                "                    \"base64Data\": \"" + base64 + "\",\n" +
                                "                        \"name\": \"" + brand + ".xlsx\",\n" +
                                "                        \"mime\": \"application/xlsx\"\n" +
                                "                    }]\n" +
                                "    },\n" +
                                "    \"stencilId\": \"STNLBIMGN\"\n" +
                                "}";

                        client.resource(mailingApi)
                                .header("x-api-key", ConfigProvider.getInstance().get(Constant.MAIL_APIKEY, ""))
                                .header("Content-Type", "application/json")
                                .accept("application/json")
                                .post(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static Date strToDate(String date) throws ParseException {
        Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(date);
        return date1;
    }

    private static String dateToStr(Date date) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(date);
    }
}