package com.neopragma.legacy.screen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

// Parameter object to handle data clumps
class ApplicantInfo {
    public String firstName;
    public String middleName;
    public String lastName;
    public String ssn;
    public String zipCode;

    public ApplicantInfo(String firstName, String middleName, String lastName, String ssn, String zipCode) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.ssn = ssn;
        this.zipCode = zipCode;
    }
}

// Extracted class for name handling
class PersonalInformation {
    private String firstName;
    private String middleName;
    private String lastName;

    public void setName(String firstName, String middleName, String lastName) {
        this.firstName = formatNamePart(firstName);
        this.middleName = formatNamePart(middleName);
        this.lastName = formatNamePart(lastName);
    }

    public void setSpanishName(String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
        this.firstName = formatNamePart(primerNombre);
        this.middleName = formatNamePart(segundoNombre);
        if (primerApellido != null) {
            StringBuilder sb = new StringBuilder(primerApellido);
            if (segundoApellido != null && !segundoApellido.isEmpty()) {
                sb.append(" ").append(segundoApellido);
            }
            this.lastName = sb.toString();
        } else {
            this.lastName = "";
        }
    }

    private String formatNamePart(String part) {
        return part == null ? "" : part;
    }

    public String formatLastNameFirst() {
        StringBuilder sb = new StringBuilder(lastName);
        sb.append(", ").append(firstName);
        if (!middleName.isEmpty()) {
            sb.append(" ").append(middleName);
        }
        return sb.toString();
    }
}

// Extracted class for SSN handling
class SSNValidator {
    private String ssn;
    private final String[] specialCases = { "219099999", "078051120" };

    public void setSsn(String ssn) {
        this.ssn = ssn.matches("(\\d{3}-\\d{2}-\\d{4}|\\d{9})") ? ssn.replaceAll("-", "") : "";
    }

    public String formatSsn() {
        if (ssn.length() != 9) return "";
        return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5);
    }

    public int validateSsn() {
        if (!ssn.matches("\\d{9}")) return 1;
        if ("000".equals(ssn.substring(0, 3)) || "666".equals(ssn.substring(0, 3)) || ssn.startsWith("9")) return 2;
        if ("0000".equals(ssn.substring(5))) return 3;
        for (String s : specialCases) {
            if (ssn.equals(s)) return 4;
        }
        return 0;
    }
}

// Extracted class for zip code service
class ZipCodeService {
    private String city;
    private String state;
    private String zipCode;

    public void setZipCode(String zipCode) throws URISyntaxException, IOException {
        this.zipCode = zipCode;
        String response = getZipCodeResponse(zipCode);
        parseCityStateFromResponse(response);
    }

    private String getZipCodeResponse(String zipCode) throws URISyntaxException, IOException {
        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("www.zip-codes.com")
                .setPath("/search.asp")
                .setParameter("fld-zip", zipCode)
                .setParameter("selectTab", "0")
                .setParameter("srch-type", "city")
                .build();
        return executeHttpRequest(uri);
    }

    private String executeHttpRequest(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
            return "";
        }
    }

    private void parseCityStateFromResponse(String response) {
        int metaOffset = response.indexOf("<meta ");
        int contentOffset = response.indexOf(" content=\"Zip Code ", metaOffset) + 19;
        contentOffset = response.indexOf(" - ", contentOffset) + 3;
        int stateOffset = response.indexOf(" ", contentOffset);
        city = response.substring(contentOffset, stateOffset);
        state = response.substring(stateOffset + 1, stateOffset + 3);
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
}

// Refactored JobApplicant class
public class JobApplicant {
    private PersonalInformation personalInfo = new PersonalInformation();
    private SSNValidator ssnValidator = new SSNValidator();
    private ZipCodeService zipCodeService = new ZipCodeService();

    public void add(ApplicantInfo info) throws URISyntaxException, IOException {
        personalInfo.setName(info.firstName, info.middleName, info.lastName);
        ssnValidator.setSsn(info.ssn);
        zipCodeService.setZipCode(info.zipCode);
        save();
    }

    private void save() {
        System.out.println("Saving to database: " + personalInfo.formatLastNameFirst());
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        startUserInputLoop();
    }

    private static void startUserInputLoop() throws URISyntaxException, IOException {
        Scanner scanner = new Scanner(System.in);
        boolean done = false;

        while (!done) {
            System.out.println("Please enter info about a job candidate or 'quit' to quit");
            System.out.println("First name?");
            String firstName = scanner.nextLine();

            if (handleQuit(firstName, scanner)) {
                done = true;
                break;
            }

            promptAndCreateApplicant(scanner, firstName);
        }
    }

    private static boolean handleQuit(String input, Scanner scanner) {
        if (input.equalsIgnoreCase("quit")) {
            System.out.println("Bye-bye!");
            scanner.close();
            return true;
        }
        return false;
    }

    private static void promptAndCreateApplicant(Scanner scanner, String firstName) throws URISyntaxException, IOException {
        System.out.println("Middle name?");
        String middleName = scanner.nextLine();

        System.out.println("Last name?");
        String lastName = scanner.nextLine();

        System.out.println("SSN?");
        String ssn = scanner.nextLine();

        System.out.println("Zip Code?");
        String zipCode = scanner.nextLine();

        ApplicantInfo info = new ApplicantInfo(firstName, middleName, lastName, ssn, zipCode);
        JobApplicant jobApplicant = new JobApplicant();
        jobApplicant.add(info);
    }
}
