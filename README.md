### Docker:

`docker build . -t geroza/angorasix.gateway`

### Maven:

`mvn clean install`

## Docker config win

https://github.com/spotify/dockerfile-maven/issues/122
https://maven.apache.org/guides/mini/guide-encryption.html#How_to_encrypt_server_passwords

## Generate Keystore:

`keytool -genkeypair -alias a6-local -keypass a6pass -validity 365 -storepass a6pass -keystore a6ks -keyalg RSA`
