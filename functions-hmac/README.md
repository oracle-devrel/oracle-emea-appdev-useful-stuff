# HMAC Authentication function for API Gateway

This is an example of how to use a function to authenticate an API Gateway MAC code, specifically an HMAC code.

## Prequisites

To compile and install this function you will need to have followed the relevant [instructions to setup the functions environment](https://docs.oracle.com/en-us/iaas/Content/Functions/home.htm) (chose if it's going to be cloud shell, local development etc. Personally I think the cloud shell is the easiest to setup)

## Create the application and it's networking

Create a VCN and subnet to put your function, see the information in the [Creating application](https://docs.oracle.com/en-us/iaas/Content/Functions/Tasks/functionscreatingapps.htm#top) Then use those instructions to create a functions application (this holds the functions) 

I would also recommend that you create a log group (The "logs" section on the left side of the applications page) for the log information

## Create the function and upload it

Assuming you've done the quick start instructions you can now upload your function. See the [Creating your function](https://docs.oracle.com/en-us/iaas/Content/Functions/Tasks/functionsuploading.htm) instructions, but as you have the functions code just make sure you are in this projects root directory and run compile and upload the function Remember to use the app name you just created.

```
fn -v deploy --app <app-name>
```

## Setup the function configuration

**IMPORTANT** Note that when a field is combined into the data to be check-summed be aware that the code will remove any whitespace around that source field before combining it, thus if there are whitespace characters around the body (e.g. a trailing newline) those must have been removed when the original "input" HMAC was calculated. See the `hmac-input-fields-trim` config option.

Use the function configuration to define the way the function will behave. This function supports the following configuration options :

  - `hmac-algorithm` Optional. The HMAC algorythm to be used to calculate the HMAC. This only supports the algorithms used in the apache commons `org.apache.commons.codec.digest.HmacUtils` class. Defaults to `HmacMD5`

  - `calculate-hmac-using` Optional. A list of fields (http headers and other items) to be used to perform the HMAC calculation. If missing defaults to only the body of the request. These are comma (`,`) separated and each one is added to the input to the HMAC calculation in the provided order by concatenating the strings with no spacing. if 'SALT' is specified then the value of the function configuration parameter `salt` is included. This allows for additional shared secrets to be applied. You cannot include the header that has the incoming HMAC (this will cause a fail with a log message). Any http headers must be in lower case, but SALT and BODY must be in upper case. For example `SALT,timestamp,BODY` with the salt set to `12345`, the timestamp of `2023-12-25-12:00:00+00:00` and a request body set to `{"data":"value"}` would result in the HMAC calculation being done against `123452023-12-25-12:00:00+00:00{"data":"value"}` Notice that there is no whitespace or other separation between the inputs by default, if you want to separate the fields then use the `separate-input-fields-using` configuration option below.

  - `hmac-secret-source` Optional. Specifies where to get the shared secret to use in the HMAC calculation, and specifically how to interpret the vault of the `hmac-secret` config parameter. The `hmac-secret-source` can be `config` or `vault` (though the code only checks for `vault` so any other value will be treated as `config`)
  
  - `hmac-secret` Must be specified. Either the OCID of a secret in a vault who's contents will be used in the HMAC calculation if `hmac-secret-source` is set to `vault`. If `hmac-secret-source` is set to anything other than `vault` then the contents of `hmac-secret` is itself used as the shared secret to be used in the HMAC calculation
  
  - `incomming-hmac-header` Must be specified. The name of the header in the incomming request that contains the pre-calculated HMAC to be used to authenticate this request. Note that this must be in lower case 
  
  - `salt` - Optional. If specified in the fields to include this can be used as an additional piece of shared configuration that is added to the HMAC calculation
  
  - `separate-input-fields-using` Optional. If specified will be used to separate the input fields, using the example if For example if `separate-input-fields-using` is set to `::` and `calculate-hmac-using` set to `SALT,timestamp,BODY` with the salt set to `12345`, the timestamp of `2023-12-25-12:00:00+00:00` and a request body set to `{"data":"value"}` would result in the HMAC calculation being done against `12345::2023-12-25-12:00:00+00:00::{"data":"value"}` The separator is only implemented between the fields, and not at the end of the input. Defaults to an empty string (so no separation between the fields)
  
  - `hmac-input-fields-trim` Optional. If specified controls if whitespace at the start / end of an input field is removed when combining the fields to calculate the HMAC. Valid options are `true` and `false`. If not specified defaults to `true`.
  

## Functions dynamic group and policies to access vault secrets 

To access the secrets in the vault (if you chose to store the hmac shared secret in the vault) you will need to create a dynamic group to identify the function. The simplest version of this is 

```
All {resource.type = 'fnfunc'}
```

This will match all functions, but you may want to restrict this further, for example adding a restriction for a specific compartment

```
All {resource.type = 'fnfunc', resource.compartment.id='compartment ocid'}
```
Would only match functions in the compartment with the specified OCID.

Once you have a dynamic group to identify the function then you need a policy that will let that group access the vault secret. This is a basic policy rule

```
allow dynamic-group functions-dg to manage secret-family in compartment hmac
```

However you may want to restrict this further.

 
## Configure a policy for APIGW to call functions

The API Gateway needs to be able to call functions. create a policy rule to allow API Gateways in the specified compartment to access functions.

```
ALLOW any-user to use functions-family in compartment hmac where ALL {request.principal.type= 'ApiGateway', request.resource.compartment.id = 'ocid1.compartment.oc1......ocid'}	
```

## Configuring the API Gateway to authenticate using the function

Follow the steps in the API Gateway [quick start documentation](https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayquickstartsetupcreatedeploy.htm) to setup the appropriate networks and gateways

Create a deployment and [add the function you created as single authentication multi argument authorizer function])https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayusingauthorizerfunction.htm) Note that you don't need to create the function itself, that's what this code does. You just need to configure the gateway to use it.

For the authorizer function setup the following function argument mapping, substituting the text in `<>` as appropriate

`request.body` -> `BODY`

`request.headers.<HMAC to validate>` -> `<name of the incoming HMAC header in the function configuration`

Note that except for the argument name `BODY` which must be in upper case, all of the other argument names must be lower case and with underscore mapped to hyphen (to `_` to `-`)

You will also need to map any request.headers to include in the HMAC calculation. It doesn't matter what the original header is called , but the name that's passed to the function (Argument name column) must match the related entry in the function configuration. So for example if the incomming header was `X-HEADER-Originator-TIMESTAMP` you coudl map it to `timestamp` if that was what you used in the `calculate-hmac-using` configuration setting for the function. This also 