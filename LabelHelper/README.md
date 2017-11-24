# Youtube Label Helper
Prerequisites for this code sample:
- Java 1.8
- Apache Maven (http://maven.apache.org)

Before running the sample, client_secrets.json must be populated with a
client ID and client secret. You can create an ID/secret pair at:

  https://code.google.com/apis/console

To build this code sample from the command line, type:

  mvn package shade:shade
  
How to run the code (after build):

  - After first execution the program will generate a new Folder 'YTHelper' in your user home directory
  - Please copy the client_secrets.json into this folder
  - Furthermore open the File 'labelHelper.properties' to configure which labels you want to generate
  - Donwload the Claim report from your CMS and paste it into the 'YTHelper' folder
  - After the program finished you will find a file called 'asset_update_label.csv' in the YTHelper folder
  - upload this file into your CMS to add the additional labels
  
Have Fun!

