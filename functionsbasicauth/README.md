# Basic Authentication function for API Gateway

This is an example of how to use a function to authenticate an API Gateway Basic auth header.

## Prequisites

To compile and install this function you will need to have followed the relevant [instructions to setup the functions environment](https://docs.oracle.com/en-us/iaas/Content/Functions/home.htm) (chose if it's going to be cloud shell, local development etc. Personally I think the cloud shell is the easiest to setup)

## Create the application and it's networking

Identify the VCN and subnet to put your function, I'd suggest using the see the information in the [Creating application](https://docs.oracle.com/en-us/iaas/Content/Functions/Tasks/functionscreatingapps.htm#top) Then use those instructions to create a functions application (this holds the functions)  If you already have an existing setup or API GW you can use the private subnet behind the API GW.

I would also recommend that you create a log group (The "logs" section on the left side of the applications page) for the log information
Once the application is created you will need to update the NSG (if using them) to allow the API GW to talk to the function. Do this by going to the application page and wheere the NSG's are listed add the NSG being used by the API GET (look at the API GW page if needed to figure this out)

## Create the function and upload it

Assuming you've done the quick start instructions you can now upload your function. See the [Creating your function](https://docs.oracle.com/en-us/iaas/Content/Functions/Tasks/functionsuploading.htm) instructions, but as you have the functions code just make sure you are in this projects root directory and run compile and upload the function Remember to use the app name you just created.

```
fn -v deploy --app basic
```

## Setup the function configuration

Use the function configuration to define the way the function will behave. This function supports the following configuration options :

  - `auth-source` Optional. The location of the actual values to check, if set to `vault` the code will assume the value in `auth-secret` is the OCID to be used to  load the secret from the vault, any other value and it will try to load the contents from the `auth-secret` config property. The default is `config` (So loading from the function config)
  
  - `auth-secret` Required, must contain either the OCID of the secrets to use (if `auth-source` is set to `vault`) OR value to check (for any onther value of the `auth-source` config) There is no default. The value can be in plain text or base 64 encoded (see the `auth-source-format` config setting)
  
  - `auth-source-format` Indicates the format of the value to check, if set to `base64` then the retrieved value (from the config or the vault secret) will be decoded form base64 before being used for compartisson checks, if it's `plaintext` then it will be used cdirectly. The default is `plaintext`
  
  - `result-cache-seconds` Indicates how long to ask the API GW to cache the results for, if not a valid number or provided does not specify a valid cache time on the returned data and will use the API GFW default (60 seconds at the time of writing) If below 60 or above 3600 will be wrapped to fit within that range as per the API GW max / min valid responses. Defaults ot no value provided and thus to the API GW default.
  
  - `testing-mode` **MUST NOT BE SET ON PRODUCTION SYSTEMS.** If set to true will include the values for auth-secret and any provided values in the log data, this is of course a very bad thing for anything other than testing the function code and logic, and **MUST NOT BE SET ON PRODUCTION SYSTEMS.** Defaults to false.
  
## Vault setup

If you are storing the auth details in a vault (`auth-source`is set to `vault` above) then create a vault (if needed) then a master signing key (again if needed) then a secret containing either the plaintext auth details OR the base64 encoded version (if using the base 64 encoded feature in the create secret make sure you set the secret type to plaintext, if adding using the plain text feature AND you are pasting the base64 encoded version then you will need to ensure that the auth type is set to base 64)

Get the OCID of the secret and put it into the `auth-secret` in the config and then set the `auth-source` to vault.
  

## Functions dynamic group and policies to access vault secrets 

To access the secrets in the vault (if you chose to store the password in the vault) Call the dynamic group `ExperientialFunctions` you will need to create a dynamic group to identify the function. The simplest version of this is 

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
allow dynamic-group ExperientialFunctions to manage secret-family in compartment experiential
```

Note that if the dynamic group was created using an identity someon (e.g. ) then the domain name needs ro be added to the DG name, for example

```
allow dynamic-group OracleIdentityCloudService/ExperientialFunctions to manage secret-family in compartment experiential
```

However you may want to restrict this further.

 
## Configure a policy for APIGW to call functions

The API Gateway needs to be able to call functions. create a policy rule to allow API Gateways in the specified compartment to access functions.

```
ALLOW any-user to use functions-family in compartment experiential where ALL {request.principal.type= 'ApiGateway', request.resource.compartment.id = 'ocid1.compartment.oc1......ocid'}	
```

## Configuring the API Gateway to authenticate using the function

Follow the steps in the API Gateway [quick start documentation](https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayquickstartsetupcreatedeploy.htm) to setup the appropriate networks and gateways

Create a deployment and [add the function you created as single authentication multi argument authorizer function](https://docs.oracle.com/en-us/iaas/Content/APIGateway/Tasks/apigatewayusingauthorizerfunction.htm) Note that you don't need to create the function itself, that's what this code does. You just need to configure the gateway to use it.

For the authorizer function setup the following function argument mapping

`request.headers.authorization` -> `authorization`

The argument name must be lower case