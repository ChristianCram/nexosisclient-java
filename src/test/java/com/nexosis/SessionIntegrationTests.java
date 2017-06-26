package com.nexosis;

import com.nexosis.impl.NexosisClient;
import com.nexosis.impl.NexosisClientException;
import com.nexosis.model.*;
import com.nexosis.util.JodaTimeHelper;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Paths;
import java.util.UUID;

public class SessionIntegrationTests {
    private static final String absolutePath = System.getProperty("user.dir") + "\\src\\test\\java\\com\\nexosis";
    private static final String productFilePath = absolutePath + "\\CsvFiles\\producttest.csv";
    private static final UUID savedSessionId = UUID.fromString("015ce131-fefb-4de2-b138-bc1d9efc4a24");
    private NexosisClient nexosisClient;

    @Before
    public void beforeClass() {
        nexosisClient = new NexosisClient(System.getenv("NEXOSIS_API_KEY"), "https://api.dev.nexosisdev.com/api/");
    }

    @Test
    public void GetBalanceWillGiveItBack() throws NexosisClientException {
        AccountBalance actual = nexosisClient.getAccountBalance();

        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.getBalance());
        Assert.assertTrue(actual.getCost().getAmount().signum() == 0);
        Assert.assertEquals("USD", actual.getBalance().getCurrency().getCurrencyCode());
    }

    @Test
    public void CreateForecastWithCsvStartsNewSession() throws FileNotFoundException, NexosisClientException {
        File initialFile = new File(productFilePath);
        InputStream inputStream = new FileInputStream(initialFile);

        SessionResponse actual = nexosisClient.getSessions().createForecast(
                inputStream,
                "sales",
                DateTime.parse("2017-03-25T00:00:00Z"),
                DateTime.parse("2017-04-25T00:00:00Z"),
                ResultInterval.DAY
        );

        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void CreateForecastWithDataDirectlyStartsNewSession() throws NexosisClientException {
        DataSetData dataSet = DataSetGenerator.Run(DateTime.now().plusDays(-90), DateTime.now(), "instances");

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        // 2016-08-01 to 2017-03-26
        SessionResponse actual = nexosisClient.getSessions().createForecast(
                dataSet,
                "instances",
                DateTime.parse("2017-03-26T00:00:00Z"),
                DateTime.parse("2017-04-25T00:00:00Z"),
                ResultInterval.DAY
        );



        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void ForcastFromSavedDataSetStartsNewSession() throws NexosisClientException {
        String dataSetName = "testDataSet-" + JodaTimeHelper.getIsoFormatter().print(DateTime.now());

        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01T00:00:00Z"),
                DateTime.parse("2017-03-26T00:00:00Z"),
                "instances");

        // setup column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        // create dataset
        nexosisClient.getDataSets().create(dataSetName, dataSet);

        // create Forecast
        SessionResponse actual = nexosisClient.getSessions().createForecast(
                dataSetName,
                "instances",
                DateTime.parse("2017-03-26T00:00:00Z"),
                DateTime.parse("2017-04-25T00:00:00Z"),
                ResultInterval.DAY
        );

        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void StartImpactWithCsvStartsNewSession() throws FileNotFoundException, NexosisClientException {
        File initialFile = new File(productFilePath);
        InputStream inputStream = new FileInputStream(initialFile);

        SessionResponse actual = nexosisClient.getSessions().analyzeImpact(
                inputStream,
                "super-duper-sale",
                "sales",
                DateTime.parse("2016-11-25T00:00:00Z"),
                DateTime.parse("2016-12-25T00:00:00Z"),
                ResultInterval.DAY
        );

        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void StartImpactWithDataDirectlyStartsNewSession() throws NexosisClientException {
        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01T00:00:00Z"),
                DateTime.parse("2017-03-26T00:00:00Z"),
                "instances"
        );

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        SessionResponse actual = nexosisClient.getSessions().analyzeImpact(
                dataSet,
                "charlie-delta-{DateTime.UtcNow:s}",
                "instances",
                DateTime.parse("2016-11-26T00:00:00Z"),
                DateTime.parse("2016-12-25T00:00:00Z"),
                ResultInterval.DAY
        );
        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void StartImpactFromSavedDataSetStartsNewSession() throws NexosisClientException {
        String dataSetName = "testDataSet-" + JodaTimeHelper.getIsoFormatter().print(DateTime.now());

        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01T00:00:00Z"),
                DateTime.parse("2017-03-26T00:00:00Z"),
                "instances");

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        nexosisClient.getDataSets().create(dataSetName, dataSet);

        SessionResponse actual = nexosisClient.getSessions().createForecast(
                dataSetName,
                "instances",
                DateTime.parse("2017-03-26T00:00:00Z"),
                DateTime.parse("2017-04-25T00:00:00Z"),
                ResultInterval.DAY
        );

        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.getSessionId());
    }

    @Test
    public void GetSessionListHasItems() throws NexosisClientException {
        SessionResponses sessions = nexosisClient.getSessions().list();

        Assert.assertNotNull(sessions);
        Assert.assertTrue(sessions.getItems().size() > 0);
    }

    @Test
    public void GetSessionResultsHasResults() throws NexosisClientException {
        SessionResult results = nexosisClient.getSessions().getResults(savedSessionId);

        if (results.getStatus() != SessionStatus.COMPLETED) {
            Assert.fail("Session is not completed. Current status is " + results.getStatus() + ". Make sure to run SessionIntegrationTests.PopulateDataForTesting() and wait for the session to complete first.");
        }

        Assert.assertNotNull(results);
        Assert.assertEquals(savedSessionId, results.getSessionId());
        Assert.assertEquals(SessionStatus.COMPLETED, results.getStatus());
    }

    @Test
    public void GetSessionResultsHasLinks() throws NexosisClientException {
        SessionResult result = nexosisClient.getSessions().getResults(savedSessionId);

        if (result.getStatus() != SessionStatus.COMPLETED) {
            Assert.fail("Session is not completed. Current status is " + result.getStatus() + ". Make sure to run SessionIntegrationTests.PopulateDataForTesting() and wait for the session to complete first.");
        }

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getLinks().size());
        Assert.assertEquals("results", result.getLinks().get(0).getRel());
        Assert.assertEquals("model", result.getLinks().get(1).getRel());
        Assert.assertEquals("data", result.getLinks().get(2).getRel());
        Assert.assertEquals("https://api.dev.nexosisdev.com/api/sessions/" + savedSessionId + "/results", result.getLinks().get(0).getHref());
        Assert.assertEquals("https://api.dev.nexosisdev.com/api/data/forecast." + savedSessionId +"/forecast/model/instances", result.getLinks().get(1).getHref());
        Assert.assertEquals("https://api.dev.nexosisdev.com/api/data/forecast." + savedSessionId, result.getLinks().get(2).getHref());
    }

    @Test
    public void GetSessionResultsWillWriteFile() throws NexosisClientException,IOException {
        String filename = absolutePath
                + "\\CsvFiles\\test-ouput-"
                + DateTimeFormat.forPattern("yyyyMMddhhmmss").withZoneUTC().print(DateTime.now())
                + ".csv";

        OutputStream output = null;

        try {
            File workingFile = new File(filename);
            workingFile.createNewFile();

            output = new FileOutputStream(filename);
            ReturnsStatus status = nexosisClient.getSessions().getResults(savedSessionId, output);

            if (status.getSessionStatus() != SessionStatus.COMPLETED) {
                Assert.fail("Session is not completed. Current status is " + status.getSessionStatus() + ". Make sure to run SessionIntegrationTests.PopulateDataForTesting() and wait for the session to complete first.");
                return;
            }

            File file = new File(filename);
            byte[] encoded = Files.readAllBytes(Paths.get(filename));
            String results = new String(encoded, StandardCharsets.UTF_8);

            Assert.assertTrue(file.length() > 0);
            Assert.assertTrue(results.startsWith("timestamp,"));
        } finally {
            if (output != null) {
             output.flush();
             output.close();
            }
            if (Files.exists(Paths.get(filename))) {
                Files.delete(Paths.get(filename));
            }
        }
    }

    @Test
    public void DeletingSessionThenQueryingReturns404() throws NexosisClientException {

        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01", DateTimeFormat.forPattern("yyyy-MM-dd")),
                DateTime.parse("2017-03-26", DateTimeFormat.forPattern("yyyy-MM-dd")),
                "instances"
        );

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        SessionResponse actual = nexosisClient.getSessions().analyzeImpact(
                dataSet,
                "charlie-delta-" + JodaTimeHelper.getIsoFormatter().print(DateTime.now()),
                "instances",
                DateTime.parse("2016-11-26", DateTimeFormat.forPattern("yyyy-MM-dd")),
                DateTime.parse("2016-12-25", DateTimeFormat.forPattern("yyyy-MM-dd")),
                ResultInterval.DAY
        );

        nexosisClient.getSessions().remove(actual.getSessionId());

        try {
            nexosisClient.getSessions().get(actual.getSessionId());
        } catch (NexosisClientException nce) {
            // SHOULD FAIL
            Assert.assertEquals(nce.getStatusCode(), HttpStatus.SC_NOT_FOUND);
            return;
        }

        Assert.fail();
    }

    @Test
    public void CheckingSessionStatusReturnsExpcetedValue() throws NexosisClientException {
        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01", DateTimeFormat.forPattern("yyyy-MM-dd")),
                DateTime.parse("2017-03-26", DateTimeFormat.forPattern("yyyy-MM-dd")),
                "instances"
        );

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        SessionResponse actual = nexosisClient.getSessions().estimateImpact(
                dataSet,
                "charlie-delta-{DateTime.UtcNow:s}",
                "instances",
                DateTime.parse("2016-11-26", DateTimeFormat.forPattern("yyyy-MM-dd")),
                DateTime.parse("2016-12-25", DateTimeFormat.forPattern("yyyy-MM-dd")),
                ResultInterval.DAY
        );

        SessionResultStatus status = nexosisClient.getSessions().getStatus(actual.getSessionId());

        Assert.assertEquals(actual.getStatus(), status.getStatus());
    }

    @Test
    public void CanRemoveMultipleSessions() throws NexosisClientException {
        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01T00:00:00Z"),
                DateTime.parse("2017-03-26T00:00:00Z"),
                "instances"
        );
        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        SessionResponse first = nexosisClient.getSessions().analyzeImpact(
                dataSet,
                "juliet-juliet-echo-1",
                "instances",
                DateTime.parse("2016-11-26T00:00:00Z"),
                DateTime.parse("2016-12-25T00:00:00Z"),
                ResultInterval.DAY
        );

        SessionResponse second = nexosisClient.getSessions().analyzeImpact(
                dataSet,
                "juliet-juliet-echo-2",
                "instances",
                DateTime.parse("2016-11-26T00:00:00Z"),
                DateTime.parse("2016-12-25T00:00:00Z"),
                ResultInterval.DAY
        );

        nexosisClient.getSessions().remove(null, "juliet-juliet-echo-", SessionType.IMPACT);

        NexosisClientException exceptionTheFirst = null;
        NexosisClientException exceptionTheSecond = null;

        try {
            nexosisClient.getSessions().get(first.getSessionId());
        } catch (NexosisClientException nce) {
            exceptionTheFirst = nce;
        }

        try {
            nexosisClient.getSessions().get(second.getSessionId());
        } catch (NexosisClientException nce){
            exceptionTheSecond = nce;
        }

        Assert.assertEquals(exceptionTheFirst.getStatusCode(), HttpStatus.SC_NOT_FOUND);
        Assert.assertEquals(exceptionTheSecond.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    }

    // @Test
    @Ignore("Only run once on a new account to setup the data for integration tests.")
    public void PopulateDataForTesting() throws NexosisClientException {
        DataSetData dataSet = DataSetGenerator.Run(
                DateTime.parse("2016-08-01T00:00:00Z"),
                DateTime.parse("2017-03-26T00:00:00Z"),
                "instances"
        );

        // setup some column metadata
        Columns cols = new Columns();
        cols.setColumnMetadata("timestamp", DataType.DATE, DataRole.TIMESTAMP);
        cols.setColumnMetadata("instances", DataType.NUMERIC, DataRole.TARGET);
        dataSet.setColumns(cols);

        SessionResponse session = nexosisClient.getSessions().createForecast(
                dataSet,
                "instances",
                DateTime.parse("2017-03-26T00:00:00Z"),
                DateTime.parse("2017-04-25T00:00:00Z"),
                ResultInterval.DAY
        );

        System.out.println(session.getSessionId() + ", " + session.getDataSetName());
    }
}