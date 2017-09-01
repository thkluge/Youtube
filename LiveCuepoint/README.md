# Youtube Midroll Inserter
Prerequisites for this code sample:
- Java 1.6
- Apache Maven (http://maven.apache.org)

Before running the sample, client_secrets.json must be populated with a
client ID and client secret. You can create an ID/secret pair at:

  https://code.google.com/apis/console

To build this code sample from the command line, type:

  mvn package shade:shade
  
How to run the code (after build):

  after first execution the program will generate a new Folder 'YTHelper' in your user home directory
  Please copy the client_secrets.json into this folder
  Furthermore open the File 'ythelper.properties' and insert the requested information
  After providing these information you can run the program again and it will check your channel in order to find current live Streams
  If it find one live stream (exactly one!) it will insert a midroll.
  
Have Fun!

