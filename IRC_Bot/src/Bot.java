import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot {

    private static Socket socket;
    private static String hostName;
    private static int port;
    private static String nick;
    private static String userName;
    private static String realName;
    private static String password;
    private static String channelName;
    private static PrintWriter out;
    private static Scanner in;
    private static String calleeMessage[];
    private static Map<String, ArrayList<String>> delayedMessage = new HashMap<>();
    private static boolean endOfWhoList;
    private static boolean isLoggedIn;
    private static String caller = "";
    //holds all IPs in the current channel
    private static HashSet<String> checkedIPs;
    private static Map<String, String> usersFromSameCountry;
    //maps the user to its query allowance left
    private static Map<String, Object[]> userAllowance;
    //holds the required parameters for monitoring the user
    // (how many times the bot was queried within an hour,
    // time of first query and did we send message that the limit is reached)
    private static Object[] allowanceParam;
    private static String ip;
    private static String country;
    private static String serverMessage;
    private static final String PRIVMSG = "PRIVMSG";
    private static final File badWords = new File("src/badWords.txt");
    private static final String IPv4_PATTERN = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    private static final String IPv6_PATTERN = "\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:)))(%.+)?\\s*";


    public static void main(String[] args) throws IOException {
        Scanner console = new Scanner(System.in);
        checkedIPs = new HashSet<>();
        usersFromSameCountry = new HashMap<>();
        userAllowance = new HashMap<>();
        allowanceParam = new Object[4];
        endOfWhoList = false;
        isLoggedIn = false;

        System.out.print("Enter nickname: ");
        nick = console.nextLine();

        System.out.print("Enter a username: ");
        userName = console.nextLine();

        System.out.print("Enter your full name: ");
        realName = console.nextLine();

        System.out.print("Enter password: ");
        password = console.nextLine();


        socket = new Socket();
        while (!socket.isBound()) {
            System.out.print("Enter host name: ");
            hostName = console.nextLine();

            try {

                System.out.print("Enter port number: ");
                port = Integer.parseInt(console.nextLine());
                socket = new Socket(hostName, port);

            } catch (NumberFormatException | ConnectException | UnknownHostException ex) {
                System.out.println("\nBad host or port");
                System.out.println("Please try again\n");
            }
        }

        System.out.print("Enter channel name: ");
        channelName = console.nextLine();


        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());


        if (!password.isEmpty()) {
            write("PASS", password);
        }

        //send the user credentials to the server
        write("NICK", nick);
        write("USER", userName + " 0 * :" + realName);
        write("JOIN", channelName);
        write("TOPIC", channelName);
        //Ask for all users on the chanel and their details.
        //Used for capturing their IPs.
        write("WHO", channelName);


        while (in.hasNext()) {
            serverMessage = in.nextLine();
            System.out.println("--> " + serverMessage);
            //the purpose of this chek is to reduce the login time
            //and to not test all informative messages sent by the server during login.
            if (isLoggedIn) {
                if (serverMessage.startsWith("PING")) {
                    String pingContents = serverMessage.split(" ", 2)[1];
                    write("PONG", pingContents);

                    //Printout the current list of countries and users
                    // from this countries at each PING respond (for informative purposes)
                    Iterator<Map.Entry<String, String>> iterator = usersFromSameCountry.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, String> entry = iterator.next();
                        System.out.println("Country: " + entry.getKey() + "\nUsers: " + usersFromSameCountry.get(entry.getKey()) + "\n");
                    }
                }
                //Only proceed if caller is captured (e.g this is not PING or other server message) and the caller
                //haven't reached the limit of allowed queries.
                caller = caller();
                if (!caller().isEmpty()) {
                    if (queryAllowed()) {

                        if (isValidIP()) {
                            if ((serverMessage.contains("QUIT") | serverMessage.contains("PART")) && !serverMessage.contains(PRIVMSG)) {//if user is leaving
                                removeCountryFellow();//remove it from the table
                            } else if (!checkedIPs.contains(ip)) {
                                addCountryFellow();
                            }
                        }


                        botIntroduction();

                        if (detectShouting()) {
                            write(PRIVMSG, caller + " :***" + caller + ", please talk nicely to the others!***");
                        } else if (detectSwear()) {
                            write(PRIVMSG, caller + " :***" + caller + ", please be nice and DO NOT use abusive words!***");
                        } else if (serverMessage.contains(" PRIVMSG " + nick + " ") && serverMessage.contains("sendTo ")) {
                            sendDelayed();
                        } else if (serverMessage.contains(" PRIVMSG " + nick + " ") && serverMessage.contains("findLocal")) {
                            showLocal();
                        } else if (serverMessage.contains(" PRIVMSG " + nick + " ") && serverMessage.contains("showCountries")) {
                            showCountries();
                        } else if (serverMessage.contains(" " + PRIVMSG + " " + nick + " ") && serverMessage.contains("showCountry ")) {
                            showCountry();
                            //if callee rejoin the chat
                        } else if ((serverMessage.contains("JOIN") && !serverMessage.contains(PRIVMSG))) {
                            //if we have message for that nick
                            if (checkForMessages(delayedMessage, caller)) {

                                write(PRIVMSG, caller + " :" + "Hi there, you have message:");

                                // send the message/s to the callee
                                for (String message : delayedMessage.get(caller)) {
                                    write(PRIVMSG, caller + " :" + message);
                                    try {//we need sleep() so we dont flood the connection
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                delayedMessage.remove(caller);
                            }
                        }
                    }
                }


            } else {
                //set isLoggedIn to true when the login is finalised
                //so we don't run all ifs while the server is sending
                //the initial data of initialising the connection
                if (serverMessage.contains("NAMES list") & !serverMessage.contains(PRIVMSG)) {
                    isLoggedIn = true;
                }
            }
        }

        in.close();
        out.close();
        socket.close();

    }

    /**
     * If the bot detect H/hello or H/hi in the public chat
     * will send greeting message with information how can caller
     * use the bot functionalities.
     * If caller send "help" message then list with all available commands
     * thir purpose and how to be used will be printed out to the caller.
     */
    private static void botIntroduction() {
        if (serverMessage.contains(PRIVMSG)) {

            try {
                if (serverMessage.contains(" " + PRIVMSG + " " + channelName + " :")) {//if the message is public
                    if (serverMessage.matches("(.*:((h|H)ello|(H|h)i)( .*)*)") && (int) userAllowance.get(caller)[3] == 0) {
                        write(PRIVMSG, caller + " :Hi there! " + nick + " is here, type \"help\" and see what I can do for you!");
                        write(PRIVMSG, caller + " :*20 queries per hour allowed");
                        allowanceParam[3] = 1;
                        userAllowance.get(caller)[3] = allowanceParam[3];
                    }
                } else if (serverMessage.matches(".*[ PRIVMSG " + nick + " ]+:help")) {//if private message with command "hi"
                    try {
                        write(PRIVMSG, caller + " :Hi there!");
                        write(PRIVMSG, caller + " :1. You can leave message to user which is not availbale at the moment. Once s/he join us I'll send it for you.");
                        write(PRIVMSG, caller + " :Command: sendTo");
                        write(PRIVMSG, caller + " :Parameters: <nick> <\"message\">");
                        write(PRIVMSG, caller + " :Example:");
                        write(PRIVMSG, caller + " :sendTo chooseUser \"Text me when you are available\"");

                        Thread.sleep(3000);
                        write(PRIVMSG, caller + " : ");
                        write(PRIVMSG, caller + " :2. You can find fellows from the same country.");
                        write(PRIVMSG, caller + " :Command: findLocal");
                        write(PRIVMSG, caller + " :Parameters: not required");
                        write(PRIVMSG, caller + " : ");

                        Thread.sleep(3000);


                        write(PRIVMSG, caller + " :3. You can see list with all countries available at the moment.");
                        write(PRIVMSG, caller + " :Command: showCountries");
                        write(PRIVMSG, caller + " :Parameters: not required");
                        write(PRIVMSG, caller + " : ");

                        Thread.sleep(3000);


                        write(PRIVMSG, caller + " :4. You can see which users are from particular country.");
                        write(PRIVMSG, caller + " :Command: showCountry");
                        write(PRIVMSG, caller + " :Parameters: name of the country");
                        Thread.sleep(3000);
                        write(PRIVMSG, caller + " :Example:");
                        write(PRIVMSG, caller + " :showCountry United Kingdom");
                        write(PRIVMSG, caller + " :*You can see country names using showCountries");
                        Thread.sleep(3000);
                        write(PRIVMSG, caller + " :---End of \"help\" list.---");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error in botIntroduction()");
                e.printStackTrace();
            }
        }
    }

    /**
     * This method was created to prevent the bot from DoS attacks.
     * "Profile" for each nickname sending messages that triggers the bot
     * is created. Number of triggers/queries, if acknowledging for reaching the limit of allowed
     * queries message was sent already to the caller,
     * time of the first trigger/query and if greeting message was sent to the caller already
     * are captured by allowanceParam.
     * After reaching the limit of queries (20 allowed for every 30 min) the bot will stop to
     * serve to this nickname.
     *
     * @return true if the caller haven't reach the limit allowed
     * or the message is not from caller but the server, else return false.
     */
    private static boolean queryAllowed() {

        LocalTime timeNow = LocalTime.now();

        //if user makes its first query - initialise the Map
        if (userAllowance.get(caller) == null) {
            allowanceParam = new Object[4];//create new "profile"
            allowanceParam[0] = 0;//num of queries attempted
            allowanceParam[1] = 0;//is notice for exhausted queries sent
            allowanceParam[2] = timeNow;//when was the first query
            allowanceParam[3] = 0;//we use this value for checking if we already send greeting message to this user
            userAllowance.put(caller, allowanceParam);

        }
        //if the message consist of command to the bot
        if (serverMessage.matches(".*[ PRIVMSG ]+ " + nick + " :((help)|(sendTo .*)|(findLocal)|(showCountries)|(showCountry .*))")
                || detectShouting()
                || detectSwear()
        ) {
            allowanceParam = userAllowance.get(caller);
            //if user reached max query allowed
            if ((int) allowanceParam[0] >= 20) {
                LocalTime timeFirstQuery = (LocalTime) userAllowance.get(caller)[2];
                //check if before user's first query left a hour. If so, then reset user's parameters
                if (timeNow.isAfter(timeFirstQuery.plusMinutes(30))) {
                    allowanceParam[0] = 0;
                    allowanceParam[1] = 0;
                    allowanceParam[2] = timeNow;
                    userAllowance.put(caller, allowanceParam);
                    return true;
                }
                //else if max allowed queries msg was not sent yet - send it (prevents flooding/DoS)
                else if ((int) allowanceParam[1] == 0) {
                    write(PRIVMSG, caller + " :***" + caller + ", you have reached the maximum allowed queries(20)! Try again after 30min.***");
                    allowanceParam[1] = (int) 1;//seting message sent to 1
                    userAllowance.put(caller, allowanceParam);//update the records
                }
                return false;
            } else {
                allowanceParam[0] = (int) allowanceParam[0] + 1;//increment caller attempts record
                userAllowance.put(caller, allowanceParam);
                return true;
            }
        }

        return true;
    }

    /**
     * This method will check if the caller message consist of capital letters only.
     *
     * @return true if the message consist of capital letters only, else false.
     */
    private static boolean detectShouting() {
        if (serverMessage.contains(" " + PRIVMSG + " " + channelName + " :")) {
            String msg = serverMessage.split(PRIVMSG + " " + channelName + " :", 2)[1];
            if (msg.matches("[\\s\\d\\W[A-Z]*]*")) {

                return true;
            }
        }
        return false;
    }

    /**
     * This method iterates over each word of the caller's message
     * and the badWords.txt file in src folder
     *
     * @return true if match found, else false
     */
    private static boolean detectSwear() {
        String word;
        try (Scanner scan = new Scanner(badWords)) {
            //if the message is in the public chat then check it
            if (serverMessage.contains(PRIVMSG + " " + channelName + " :")) {
                String msg = serverMessage.split(PRIVMSG + " " + channelName + " :", 2)[1];
                String[] message = msg.split(" ");
                while (scan.hasNextLine()) {
                    word = scan.nextLine();
                    for (String s : message) {
                        if (s.equals(word)) {

                            return true;
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | FileNotFoundException e) {
            System.out.println("badWords file not found!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This method prints out to the caller all nicknames
     * from this chat, which are from the same country as s/he is.
     */
    private static void showLocal() {

        Iterator<Map.Entry<String, String>> iterator = usersFromSameCountry.entrySet().iterator();
        write(PRIVMSG, caller + " :These people are from the same country as you:");
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (usersFromSameCountry.get(entry.getKey()).contains(caller)) {
                write(PRIVMSG, caller + " :" + usersFromSameCountry.get(entry.getKey()));
            }
        }
    }

    /**
     * This method prints out to the caller the countries
     * of all chat participants
     * Could be used as reference for showCountry method.
     */
    private static void showCountries() {
        String countries = new String();
        Set<String> countriesSet = usersFromSameCountry.keySet();
        for (String key : countriesSet) {
            countries += key + ", ";
        }
        write(PRIVMSG, caller + " :List of all countries in this chat:");
        write(PRIVMSG, caller + " :" + countries);

    }

    /**
     * This method takes the user input (the parameter of showCountry function),
     * searches usersFromSameCountry map and prints to the caller whatever is found
     * under that key (country name).
     */
    private static void showCountry() {

        int beginIndex = serverMessage.indexOf("showCountry") + "showCountry".length() + 1;
        String country = serverMessage.substring(beginIndex);
        String nickNames = usersFromSameCountry.get(country);
        write(PRIVMSG, caller + " :All nicknames from " + country + ":");
        if (nickNames != null) {
            write(PRIVMSG, caller + " :" + nickNames);
        } else {
            write(PRIVMSG, caller + " :There are no users from " + country + " at the moment.");
        }

    }

    /**
     * This method either adds the country of the current caller
     * to usersFromSameCountry or if the country name is already there
     * ads his/hers nickname to the rest of nicknames from the same country.
     */
    private static void addCountryFellow() {
        country = getLocation(ip);
        if (country.equals("Undefined")) {
            System.out.println("UNDEFINED IP: " + "\"" + ip + "\"");
            return;
        }
        checkedIPs.add(ip);//add the IP to Set of IPs so we don't check its location more than once
        String currentListOfUSers = usersFromSameCountry.get(country);//copy all users from this country
        if (currentListOfUSers != null && !currentListOfUSers.isEmpty()) {//if usersFromSameCountry already had key with the given country
            usersFromSameCountry.put(country, currentListOfUSers + ", " + caller);//take all existing users and append the new one
        } else {
            usersFromSameCountry.put(country, caller);//else create new entry and add the user
        }
    }

    /**
     * This method iterates over usersFromSameCountry.
     * If the caller name (the initiator of the given message)
     * is found in the map will be deleted from the map
     * and from the checkedIPs set.
     */
    private static void removeCountryFellow() {
        //If the user is in the Map, find and assign to country his/her's country
        Iterator<Map.Entry<String, String>> iterator = usersFromSameCountry.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (usersFromSameCountry.get(entry.getKey()).contains(caller)) {
                country = entry.getKey();
            }
        }

        String updatedTable = usersFromSameCountry.get(country);
        if (updatedTable != null) {
            if (updatedTable.contains(caller + ", ")) {//if there are more nicks and the given one is first
                updatedTable = updatedTable.replace(caller + ", ", "");
                usersFromSameCountry.put(country, updatedTable);
            } else if (updatedTable.contains(", " + caller)) {//if the given one is not first
                updatedTable = updatedTable.replace(", " + caller, "");
                usersFromSameCountry.put(country, updatedTable);
            } else if (updatedTable.equals(caller)) {//if the given one is the only one
                updatedTable = updatedTable.replace(caller, "");
                usersFromSameCountry.put(country, updatedTable);
            }
            if (updatedTable.isBlank()) {
                usersFromSameCountry.remove(country);
            }
            checkedIPs.remove(ip);
        }
    }

    /**
     * This method search for IP addresses within the server message.
     *
     * @return true if IP is found, else false.
     */
    private static boolean isValidIP() {

        Pattern pattern = Pattern.compile(IPv4_PATTERN);
        Matcher matcher = pattern.matcher(serverMessage);
        if (matcher.find()) {
            ip = matcher.group().trim();
            return true;
        }
        pattern = Pattern.compile(IPv6_PATTERN);
        matcher = pattern.matcher((serverMessage));
        if (matcher.find()) {
            ip = matcher.group().trim();
            return true;
        }

        return false;
    }


    /**
     * This method extracts the nickname of the message sender
     * or nickname from the server messge (if the message contains nickname)
     *
     * @return the name of the message initiator/the nickname this message
     * is related to or empty string if no name.
     */
    private static String caller() {

        try {//because the list of users returned from "WHO" request (line 99)
            //doesn't contain "!" we need to serch the nickname differently
            if (!endOfWhoList) {
                if (serverMessage.contains("WHO list")) {
                    endOfWhoList = true;
                    usersFromSameCountry.remove("Undefined");
                    return "";
                }
                caller = serverMessage.split(" ")[4];
                if (caller.startsWith("~")) {
                    caller = caller.substring(1);
                }
            } else {//for the rest of the messges we can identify the nickname searching "!"
                caller = serverMessage.split("!")[0];
                //if the message don't contain "!" we don't have caller name
                if (caller.length() == serverMessage.length()) {
                    return "";
                }
                caller = caller.substring(1);
            }
            //split method might throw error
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error in caller()!");
            e.printStackTrace();
        }
        return caller;
    }

    /**
     * The purpose of this method is to allow signed users to send
     * message/s to signed out users. If the sender sign out too and the receiver
     * sign in, the bot will send the messge to the receiver.
     * Example - sendTo guest "I'm in #chatName"
     */
    private static void sendDelayed() {
        calleeMessage = new String[2];
        try {//syntax - sendTo callee "message"
            //take receiver name and message
            String sendTo = serverMessage.split("sendTo", 2)[1];
            calleeMessage = sendTo.split("\"", 2);
            String callee = calleeMessage[0].trim();//receiver name
            if(calleeMessage[1].isEmpty()){
                throw new ArrayIndexOutOfBoundsException();
            }
            //if calles is already in the map add the message
            if (delayedMessage.containsKey(callee)) {
                //add receiver and message to the Map(including the sender's nickname)
                delayedMessage.get(callee).add("\"" + calleeMessage[1] + " --> left from " + caller);
                write(PRIVMSG, caller + " :Message Recorded");
            } else {//else create entry with the callee name and add the message
                delayedMessage.put(callee, new ArrayList<>());
                delayedMessage.get(callee).add("\"" + calleeMessage[1] + " --> left from " + caller);
                write(PRIVMSG, caller + " :Message Recorded");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error in sendDelayed()");
            e.printStackTrace();
            write(PRIVMSG, caller + " :Message NOT recorded!");
            write(PRIVMSG, caller + " :Use neutral (vertical) quotation marks and make sure you pass parameters correctly.");
            write(PRIVMSG, caller + " :Read section 1 from the help menu or type \"help\" to receive it.");
        }
    }


    /**
     * Iterates over the Map with messages to check if the callee is there and has a message
     *
     * @param delayedMessage Map holding all messegaes left from one user for another
     * @param caller         the user which did left messeage
     * @return true if callee has message, otherways - false
     */
    private static boolean checkForMessages(Map<String, ArrayList<String>> delayedMessage, String caller) {
        //Create iterator over the HashMap
        Iterator<Map.Entry<String, ArrayList<String>>> iterator = delayedMessage.entrySet().iterator();
        //iterate over the HasMap
        while (iterator.hasNext()) {
            Map.Entry<String, ArrayList<String>> entry = iterator.next();

            if (caller.equals(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used for sending commands to the server
     *
     * @param command the command
     * @param message the parameter for the command
     */
    private static void write(String command, String message) {
        String fullMessage = command + " " + message;
        System.out.println("<-- " + fullMessage);
        out.print(fullMessage + "\r\n");
        out.flush();
    }

    /**
     * This method connects with IP location API
     *
     * @param ip is he ip which will be checked
     * @return IP location (country name)
     * NOTE: 1000 queries per day allowed
     */
    private static String getLocation(String ip) {
        String url = "https://ipapi.co/" + ip + "/country_name/";
        StringBuffer response = new StringBuffer();

        try {
            URL objUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) objUrl.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println(
                    "GET response code: " + responseCode + " from ip: \"" + ip + "\""
            );

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            }
            return response.toString();

        } catch (IOException e) {
            System.out.println("getLocation() error");
            e.printStackTrace();
        }

        return response.toString();
    }
}
