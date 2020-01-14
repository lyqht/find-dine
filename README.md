# Find Dine

## Requirements

- The smartphone has to support the ranging standard by hardware. Newer smartphones using e.g. a Qualcomm Snapdragon 820 CPU should do that.
- Android P installed on the smartphone.
- The access point has to support the IEEE 802.11mc FTM standard

### The Hardware used in Demo

- 1 Nest router
- 4 Nest access points
- 2 Google Pixel 3A

## Setup 





## Detection of Access Points with Wi-Fi RTT

Wi-Fi RTT was introduced in Android 9 (API level 28). ([link](https://developer.android.com/guide/topics/connectivity/wifi-rtt#implementation_differences_based_on_android_version))
- On devices running Android 9, you need to have access to pre-determined access point (AP) locations data in your app.
- On devices running Android 10 (API level 29) and higher, AP location data can be represented as `ResponderLocation` objects, which include latitude, longitude, and altitude. For Wi-Fi RTT APs that support Location Configuration Information/Location Civic Report (LCI/LCR data), the protocol will return a `ResponderLocation` object during the ranging process. 

`ResponderLocation` is both a Location Configuration Information (LCI) decoder and a Location Civic Report (LCR) decoder for information received from a Wi-Fi Access Point (AP) during Wi-Fi RTT ranging process. ([link](https://developer.android.com/reference/android/net/wifi/rtt/ResponderLocation))
- This is based on the IEEE P802.11-REVmc/D8.0 spec section 9.4.2.22, under Measurement Report Element. Subelement location data-fields parsed from separate input LCI and LCR Information Elements are unified in this class.
- This feature allows apps to query APs to ask them for their position directly rather than needing to store this information ahead of time. So, your app can find APs and determine their positions even if the APs were not known before, such as when a user enters a new building.

### Challenges
- Google Nest Home don’t support `ResponderLocation`. 
  - But even if they do support it, we also need configure the AP to include the information to be responded to app.
-  Google Nest Wifi seems to be requires a connection to a working modem

### Suggested Structure of Access Point Data
```
Access Points = [{
 floor:
 lat:
 long:
 MAC:
 etc..
}]
```

When interpreting RangingRequest, 
- we do `request.macAddress` and map it with our hardcoded data
- then we can calculate the user position using some calculation with `request.distanceMm`  & our AP location

## Relevant links
- https://medium.com/@plinzen/perform-wifi-round-trip-time-measurements-with-android-p-9ffc5277ac6a
- https://developer.android.com/guide/topics/connectivity/wifi-rtt#implementation_differences_based_on_android_version
- https://developer.android.com/reference/android/net/wifi/rtt/ResponderLocation