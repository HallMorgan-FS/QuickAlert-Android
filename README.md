# QuickAlert - Emergency Safety App

## Description

QuickAlert is a powerful and intuitive emergency safety app designed to
provide immediate assistance in times of danger. With just two simple
buttons, you can swiftly send distress signals to both emergency
services and your designated contacts.

## Key Features

-   Fast and efficient emergency response with just two buttons
-   Instantly sends an SMS message with your location to 911 and
    emergency contacts
-   Safely notifies designated contacts of potential danger with
    discreet SMS messages
-   Precise location tracking for accurate assistance
-   Customizable emergency contacts for personalized safety networks
-   Intuitive and user-friendly interface for quick access during
    critical situations

## Technologies Used

-   Java
-   Android Studio
-   Firebase Authentication
-   Firebase Realtime Database
-   Geocoding for location services

## Installation and Setup

1.  Clone the repository
2.  Open the project in Android Studio
3.  Install any required dependencies
4.  Run the app on an emulator or physical device

## Usage

After installation, open the app and follow the on-screen instructions
to set up your emergency contacts. In case of an emergency, tap the
relevant button to send an alert.

## Code Examples
// Code to send SMS
```
if (buttonTag.equals(\"SOS\")){
    String sos911message = HelperMethods.*get911SOSMessage*(name,
    readableAddress, num_contacts) + HelperMethods.*getSignature*(name);
    String sosContactMessage = HelperMethods.*getContactSOSMessage*(name,
    readableAddress, num_contacts) + HelperMethods.*getSignature*(name);
    //Send SOS 911 message to developers number for testing
    smsManager.sendTextMessage(\"+19108845882\", null, sos911message, null,
    null);
    for (Contacts contact : contacts){
        smsManager.sendTextMessage(contact.getNumber(), null, sosContactMessage,null, null);
    }
    alertMessage = \"The distress message with your location has been sent
    911 and all designated contacts. Stay safe, help is on the way.\\nThis
    box will close in 3 minutes\";
  }
```

