package com.flipkart.ads.report;

import com.flipkart.ad.dp.ConfigProvider;
import com.flipkart.ad.dp.Constant;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
import java.util.*;


/**
 * Created by rahul.sachan on 28/06/17.
 */

//ssh -f -N -L 7788:10.33.145.239:3306 10.32.246.118
//ssh -f -N -L 7766:10.32.149.12:3306 10.32.246.118
//ssh -f -N -L 7755:10.33.10.193:3306 10.32.246.118
public class LeadGeneration {

    public static void main(String[] args) throws IOException, SQLException, JSONException, ParseException {
        Client client = Client.create();
        ConfigProvider cp = new ConfigProvider();
        String mailingApi = "http://" + ConfigProvider.getInstance().get(Constant.MAIL_HOST, "") + "/v1/send/email/lead-reports";// + cp.get(Constant.MAIL_USER, "");
        String stencilId = ConfigProvider.getInstance().get(Constant.MAIL_STENCILID, "");
        String XLXS_PWD = ConfigProvider.getInstance().get(Constant.XLXS_PWD, "");
        String bcc = ConfigProvider.getInstance().get(Constant.MAIL_BCC, "");
        String to = "rahul.sachan@flipkart.com";
        PreparedStatement pr;
        String RPTQUERY = null;
        String BRANDQUERY = null;
        String mailSub = "Lead generation Report";
        String mailBody = null;

        List<HashMap<String, String>> formList = new ArrayList<>();
        HashMap<String, String> cmpIdNameMap = new HashMap<String, String>();
        Set<String> headers = new HashSet<>();

//        String repType = args[0];
//        String year = args[1];
//        String month = args[2];
//        String day = args[3];
//        String hour = args[4];

        String repType = "daily";
        String year = "2017";
        String month = "11";
        String day = "11";
        String hour = "12";


//        Connection con2 = JDBCUtill.getJdbcConnection(cp.get(Constant.DB_CONFIGURATION_URL, ""), cp.get(Constant.DB_CONFIGURATION_USER, ""), cp.get(Constant.DB_CONFIGURATION_PASSWORD, ""));

        Connection con2 = JDBCUtill.getJdbcConnection("jdbc:mysql://127.0.0.1:7755/demand_report", "demand", "demand123");

        String endTime = year + "/" + month + "/" + day;
        Date endTime_inDate = null;
        String startTime = null;
        try {
            endTime_inDate = strToDate(endTime);
        } catch (ParseException e) {
            System.out.println("Error while converting to str To Date" + e.getLocalizedMessage());
        }

        if (repType.equals("hourly")) {
            BRANDQUERY = "select * from demand_report.advertiser_mailing_hourly where status=1";
        } else if (repType.equals("daily")) {
            BRANDQUERY = "select * from demand_report.advertiser_mailing_daily where status=1";
        } else {
            System.out.println("Please pass correct parameter");
        }

        pr = con2.prepareStatement(BRANDQUERY);
        ResultSet rs = pr.executeQuery();
        while (rs.next()) {
            String brand = rs.getString("brand");
            String advName = rs.getString("advertiser");
            String mailIds = rs.getString("email");
            String cmpids = rs.getString("cmpids");
            int backday_report = rs.getInt("backday_report");

            to = getMailTolist(mailIds);


            if (repType.equals("hourly")) {
                Date daysAgo = new DateTime(endTime_inDate).minusDays(backday_report).toDate();
                endTime = dateToStr(endTime_inDate) + "T" + hour + ":00:00";
                startTime = dateToStr(daysAgo) + "T00:00:00";
                mailBody = "Please find attached leads till " + year + "-" + month + "-" + day + ":" + hour;
            } else if (repType.equals("daily")) {
                Date daysAgo = new DateTime(endTime_inDate).minusDays(backday_report).toDate();
                endTime = dateToStr(endTime_inDate) + "T23:59:59";
                startTime = dateToStr(daysAgo) + "T00:00:00";
                mailBody = "Please find attached leads till " + year + "-" + month + "-" + day;
            }

            int numberOfpages = getTotalPage(client, cmpids, startTime, endTime, 50000);
            int pagenumber = 0;
            while (pagenumber < numberOfpages) {
                formList = getFormData(client, cmpids, startTime, endTime, 50000, 50000 * pagenumber);
                Iterator<HashMap<String, String>> itr = formList.listIterator();
                while (itr.hasNext()) {
                    headers.addAll(itr.next().keySet());
                }

                try {
                    Workbook wb = new SXSSFWorkbook();
                    StyleSheet css = new StyleSheet();
                    POIFSFileSystem fileSystem = new POIFSFileSystem();
                    Sheet sheet = wb.createSheet();
                    int rowNumber = 0;
                    int cellNumber = 0;
                    Row headerRow = sheet.createRow(rowNumber++);
                    for (String header : headers) {
                        Cell fieldCell = headerRow.createCell(cellNumber++);
                        fieldCell.setCellValue(header);
                        fieldCell.setCellStyle(css.getHeadingCss(wb));
                    }

                    itr = formList.listIterator();
                    while (itr.hasNext()) {
                        Row r = sheet.createRow(rowNumber++);
                        HashMap<String, String> formdata = new LinkedHashMap<>();
                        formdata = itr.next();
                        cellNumber = 0;
                        for (String key : headers) {
                            Cell cell = r.createCell(cellNumber++);
                            if (formdata.containsKey(key)) {
                                cell.setCellValue(formdata.get(key).toString());
                            } else {
                                cell.setCellValue("-");
                            }
                        }
                    }
                    EncryptionInfo info = new EncryptionInfo(EncryptionMode.standard);
                    Encryptor enc = info.getEncryptor();
                    enc.confirmPassword(XLXS_PWD);
                    OutputStream encryptedDS = enc.getDataStream(fileSystem);
                    wb.write(encryptedDS);
                    FileOutputStream fos = new FileOutputStream("/tmp/example.xlsx");
                    fileSystem.writeFilesystem(fos);
                    String base64 = DatatypeConverter.printBase64Binary(Files.readAllBytes(
                            Paths.get("/tmp/example.xlsx")));
                    mailSub = "Lead generation Report of " + brand;
                    sendMail(client, to, bcc, base64, mailingApi, mailBody, mailSub, brand);
                    pagenumber++;
                } catch (Exception e) {
                    System.out.println("System failed to send mail for : " + brand);
                }

            }
        }
    }

    private static HashMap<String, String> getData(JSONObject o) throws JSONException, IOException {
        String cnv_formdata = o.getString("leadJson");
        HashMap<String, String> leads = new LinkedHashMap<>();
        JSONObject jsonObj = new JSONObject(cnv_formdata);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> cnvFormatData = objectMapper.readValue(cnv_formdata, new TypeReference<Map<String, String>>() {
        });
        leads.put("campaignId", o.getString("campaignId").toString());
        leads.put("cnvDateTime", o.get("cnvDateTime").toString());
        for (String key : cnvFormatData.keySet()) {
            leads.put(key, cnvFormatData.get(key));
        }
        return leads;
    }


    private static String getMailTolist(String mailIds) throws JSONException {
        String[] Mails = mailIds.split(",");
        JSONArray ja = new JSONArray();
        for (String m : Mails) {
            JSONObject jo = new JSONObject();
            jo.put("name", "");
            jo.put("address", m);
            ja.put(jo);
        }
        return ja.toString();
    }

    private static String getCmpString(String cmpids) {
        String[] cmplist = cmpids.split(",");
        String sqlcmplist = "";
        for (String cmpid : cmplist) {
            sqlcmplist = sqlcmplist + "'" + cmpid + "',";
        }
        if (sqlcmplist != null && sqlcmplist.length() > 2) {
            sqlcmplist = sqlcmplist.substring(0, sqlcmplist.length() - 1);
        }
        return sqlcmplist;
    }

    private static Date strToDate(String date) throws ParseException {
        Date date1 = new SimpleDateFormat("yyyy/MM/dd").parse(date);
        return date1;
    }

    private static String dateToStr(Date date) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(date);
    }

    private static List<HashMap<String, String>> getFormData(Client client, String entityId, String startTime, String endTime, int pageSize, int pageOffset) throws JSONException, IOException {
        WebResource webResource = client.resource("http://10.47.5.108:80/v1/leads/campaign?entityId=" + entityId + "&pageSize=" + pageSize + "&pageOffset=" + pageOffset + "&startDate=" + startTime + "&endDate=" + endTime);
        ClientResponse response = webResource.type("application/json")
                .get(ClientResponse.class);
        JSONObject output = new JSONObject(response.getEntity(String.class));
        JSONArray formdata = output.getJSONObject("response").getJSONArray("leadList");
        List<HashMap<String, String>> formList = new ArrayList<>();
        for (int i = 0; i < formdata.length(); i++) {
            HashMap<String, String> dd = new HashMap<String, String>();
            JSONObject jo = formdata.getJSONObject(i);
            try {
                dd = getData(jo);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            formList.add(dd);
        }
        return formList;
    }

    private static int getTotalPage(Client client, String entityId, String startTime, String endTime, int pageSize) throws JSONException {
        WebResource webResource = client.resource("http://10.47.5.108:80/v1/leads/campaign?entityId=" + entityId + "&pageSize=" + pageSize + "&pageOffset=0&startDate=" + startTime + "&endDate=" + endTime);
        ClientResponse response = webResource.type("application/json")
                .get(ClientResponse.class);
        JSONObject output = new JSONObject(response.getEntity(String.class));
        int numberOfpages = output.getJSONObject("response").getInt("totalPage");
        return numberOfpages;
    }

    private static void sendMail(Client client, String to, String bcc, String base64, String mailingApi, String mailBody, String mailSub, String brand) {
        String data = "{\n" +
                "    \"sla\": \"H\",\n" +
                "    \"channelInfo\": {\n" +
                "                \"type\": \"EMAIL\",\n" +
                "                \"appName\": \"flipkart\",\n" +
                "                \"to\": " + to + ",\n" +
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

}