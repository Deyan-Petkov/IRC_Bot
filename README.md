# IRC-Bot

#### Bot functionalities:
1. Send message to user which is
not available at the moment (once s/he log in) on behalf of user which is
logged in but about to leave.
2. Help users find other users in the chat which are from the same country,
or help exploring which users belong to which country
3. Detect abusive language

#### Bot functionalities and available commands:

1. **H/hi, H/hello** ---> in the public chat will return personal greeting message to the caller
giving guidance how to use the bot.
2. **help** ---> will print out all commands the bot responds to.
3. **sendTo** ---> parameters <nick> <message> . This command saves a message
left from logged in user. This message is for user which is not signed in
the moment, but once sign in again will receive the message
(e.g. sendTo unavailableUser "I am in #chatName").
4. **findLocal** ---> shows all nicknames from the same country as the caller.
5. **showCountries** ---> prints out to the caller the countries of
all chat participants.
Could be used as reference for showCountry's required parameter.
6. **showCountry** ---> show nicks from the given country.
7. Detect abusive words in the public chat and send private message
to the abuser.
8. Detect if user shouts in the public chat (using only capital letters)


*  This bot was build to work and tested in chat.freenode.net server. As server
messages syntax is quite similar, you can expect it to work in many other servers
*  Be careful because the bot will prompt with personal message each user who uses
abusive language, and some people get annoyed :(
*  Users are allowed up to 20 queries for every 30min (prevents DoS/flooding attack)
*  The countries of the users are indentified on IP basis. If particular IP is hiden
behind DNS it will not be resolved.
