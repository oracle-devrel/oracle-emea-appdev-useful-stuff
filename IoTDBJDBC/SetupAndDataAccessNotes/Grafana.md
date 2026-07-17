#Grafana
Grafana is a very widley used display tool for time series (and other) data.

The development team are building a grafana plugin that can be used to connect to the time series DB
this is available at https://github.com/oracle-samples/oracle-telemetry-streaming

## Creating a VM to run Grafana

###Network decisions
If using a network that has been setup to do other things then you **may** want to consider a separate subnet to provide greater routing and security controls. This is especially true if other subnets in the network are not configured to host VM's public internet access.

###Gateways
For incoming access via the internet (as opposed to via a ssh tunnel over a bastion) ensure that there is an internet gateway configured on the VCN. There will need to be route entries configured for this as well (that's later)

Note that if you have a nat gateway already there (used by private subnets for outgoing internet access) then you will need to setup a new route table for the subnet the grafana VM is on (as the default 0.0.0.0/0 target can't be used multiple times in the same route table.


###Security
You will need a security list with the following, if adding to an existing list then it's probably only the grafana port you need to add:

####Ingress
ICMP from 0.0.0.0 (possibly can be limited to the specific IP's you'll be accessing it from) for type 3, code 4
ICMP from the VCN IP range for type 3
A non trusted port of your choice (e.g. 4242 which is used later on here, but chose what you prefer) from the machines you want to be able to connect to the grafana instance (possibly 0.0.0.0/0) 
ssh (22) from the machines you want to be able to connect to the instance (possibly 0.0.0.0/0) alternatively you could allow it only local to the VCN and use a bastion.
####Egress
to 0.0.0.0/0 all protocols

You can also 
###Routes
Is using internet access check you can use the default (if setup like this) or create a **new** route table with the following
All <Region> service via the svc gateway
outgoing to 0.0.0.0/0 all protocols (via internet gateway) 
If you are going to connect using an ssh tunnel you'll probably be using a natgw (needed for outbound access to load SW and so on) 
###Subnet
Create a subnet, make sure than public IP's are allowed Associate the sec lists and make sure the Subnet has the default route table (if that is setup with an internet gateway) **or** the one you setup above to allow internet access
###VM Creation
You will need a basic VM running AMD / Intel and OL8 or 9
Put it on the Subnet you just created
Make sure it's requesting an IP
Setup an ssh key

##Prep for grafana instalation
ssh to the worker, directly or via a bastion depending how you set it up
make sure you have external connectivity - e.g.  curl https://www.google.com

update the entire OS image
sudo dnf update

install oci cli
sudo dnf -y install oraclelinux-developer-release-el9
sudo dnf install python39-oci-cli

Configure the .oci directory, copy over your existing credentials or setup by hand with oci (it will ask you to set it up and explain how)

install git (needed to get the code) - note that curl should alrerady be installed and the dev tools probabaly came with the image
sudo dnf install -y curl git
sudo dnf group install -y "Development Tools"

Basically follow the instructions on the oracle-telemetry-streaming git page
in summary install :
    
    nvm, npm, node.js and yarn (not sure if this is needed for real or just development)
    go 1.24 (or later ?)
    oracle instant client
 
### NVM / NPM / node.js install
These seem to work
https://oneuptime.com/blog/post/2026-03-04-install-nodejs-using-nvm/view

once installed switch to the required node.js version
nvm install 16
nvm use 16
nvm alias default 16

Use npm to install yarn
npm install --global yarn@1

### Go install
follow the instruction at https://go.dev/doc/install (remember to chose the Linux instructions)
example - (change file names as appropriate)
wget https://go.dev/dl/go1.26.5.linux-amd64.tar.gz
gunzip go1.26.5.linux-amd64.tar.gz
tar -xf go1.26.5.linux-amd64.tar
move it into place (remove older versions if needed)
sudo mv go /usr/local
Update the library path and command path
set the path in the ~/.bash_profile
echo export PATH=$PATH:/usr/local/go/bin >> ~/.bash_profile
remove the download
rm go1*

##DB setup
###DB Drivers
Download Oracle Instant Client RPM
wget https://download.oracle.com/otn_software/linux/instantclient/2380000/oracle-instantclient-basiclite-23.8.0.25.04-1.el9.x86_64.rpm

Install the drivers package
sudo dnf install -y oracle-instantclient-basiclite-23.8.0.25.04-1.el9.x86_64.rpm

remove the package
rm oracle-instant*.rpm

update the path
echo 'export LD_LIBRARY_PATH=/usr/lib/oracle/23/client64/lib:$LD_LIBRARY_PATH' >> ~/.bash_profile
echo 'export PATH=/usr/lib/oracle/23/client64/bin:$PATH' >> ~/.bash_profile

###DB Wallet
Get the DB wallet files (you will need the oci cli setup and configured for this) or grab them to your systems and scp them to the vm, the command for that will be like
To download directly onto the VM
oci db autonomous-database generate-wallet \
  --autonomous-database-id <autonomous_database_ocid> \
  --password '<wallet_password>' \
  --generate-type SINGLE \
  --file Wallet.zip

Alternatively download them to your laptop and copy them over

scp -i ~/.ssh/id_rsa \
  -o 'ProxyCommand=ssh -i ~/.ssh/id_rsa -W %h:%p -p 22 <bastion session ocid>>@host.bastion.uk-london-1.oci.oraclecloud.com' \
  -P 22 \
  ./Wallet_Telemetry.zip \
  opc@<in subnet grafana IP>>:/home/opc/Wallet.zip
  
move the DB wallet to a suitable location and unzip it.
e.g.
mkdir tsdbwallet
mv Wallet.zip tsdbwallet
cd tsdbwallet
unzip Wallet.zip
cd ~

look in the tnsnames.ora for the DB connection name (e.g. telemetry_high)

set the wallet file variable to the location
echo export TNS_ADMIN=/home/opc/tsdbwallet >> ~/.bash_profile


###Update the wallet for Grafana
For some reason the way the wallet is used in the Grafana plugin seems to be different from the way it's used in the zipped walled file with the sql command. This means that by default wallet files (when unzippd) needs to be changed.

In the wallet file edit the sqlnet.ora file, change the DIRECTORY entry to point to the location of the unzipped wallet (that baffles me as surnely the TNS_NAMES environment variable points to this, but the database connectivity is somewhat of a black art !

The new version will look like ```WALLET_LOCATION = (SOURCE = (METHOD = file) (METHOD_DATA = (DIRECTORY="/home/opc/tsdbwallet")))`` (assuming that the wallet was unzipped to /home/opc/tsdbwallet)


###Get Java (needed for sqlcl for testing purposes)
get the relevant Java version

sudo dnf install java-25-openjdk-devel --assumeyes
try to switch to the new version (if using the Oracle alternatives system)
sudo alternatives --config java
sudo alternatives --config javac

List older versions of java if needed
alternatives --list | grep java
uninstal unneeded ones (e.g. jre_17)
sudo  alternatives --remove-all jre_17
sudo  alternatives --remove-all jre_openjdk 
See if there are older versions of java installed using dnf
dnf list --installed 'java-*-openjdk*' 'jdk-*'
If so remove them 
sudo dnf remove  java-17-openjdk-headless

###Get sqlcl
wget https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip
unzip sqlcl-latest.zip

put it somewhere
sudo mv sqlcl /usr/local

Add it to the path
echo 'export PATH=/usr/local/sqlcl/bin:$PATH' >> ~/.bash_profile

###Load in this new config

log out and back in or reload
source ~/.bash_profile

###test it all works
(assuming the wallet is where it's been put above)
sql -cloudconfig ./tsdbwallet/Wallet.zip <tsdb query user>/<tsdquery user pw>@<dbname>_high
you can check the connection works form the machine

to check that the sqlinit.ora changes work (i.e that grafana can connect) make sure that TNS_ADMIN is set (above)
sql <tsdb query user>/<tsdquery user pw>@<dbname>_high

select count(*) from telemetry_metrics ;

##Get a certificate
If you want to go through the process of setting up a DNS name and getting a proper cet, but otherwise you can use a combination of small step and nip to generate a root cert and self signed cert

###Using small step
Create a directory
mkdir ~/smallstep
cd ~/smallstep

Look at the downloads page to work out the URL for the latest download
Open https://github.com/smallstep/cli/releases/latest in a web browser (it will rediect to a page for the current latest
Locate the download you want in the list based on OS and processor

Download that link in the VM
wget -O step.tar.gz <the URL>

unpack it
tar -xf step.tar.gz

move the executable 
mv step_*/bin/step .

delete the downloads
rm step.tar.gz
rm -rf step_*

make sure it's executable
chmod ogu+x step

create a root cert
./step certificate create root.cluster.local root.crt root.key --profile root-ca --no-password --insecure --kty=RSA

Set EXTERNAL_IP to the IP address of the VM - get that from OCI (Web UI, cli etc.)
EXTERNAL_IP=<IP address>
Create the cert
./step certificate create grafana.$EXTERNAL_IP.nip.io tls-grafana.crt tls-grafana.key --profile leaf --not-after 8760h --no-password --insecure  --kty=RSA --ca root.crt --ca-key root.key

###Update the firewall rules
open up the post, use the port you've chosen - I'm using 4242 here as per my examples above
sudo firewall-cmd --permanent --add-port=4242/tcp
sudo firewall-cmd --reload

##Build the grafana plugin
Follow the Download and build instructions at the plugin site https://github.com/oracle-samples/oracle-telemetry-streaming/blob/main/README.md

Note that you need to do the yarn install and yarn build, unclear if the other yarn steps are required

##Download Grafana
Follow the Download instructions at the plugin site https://github.com/oracle-samples/oracle-telemetry-streaming/blob/main/README.md

After untaring / gunziping rename the resulting directory to grafana

###Basic Grafana configuration
Do a basic setup, production  configs would be way more complicated

go to the config directory
cd grafana/conf

Use an editor of your choice and edit the defaults.ini file

In the server section:
Locate the protocol line, set it to https
Locate the http_port and set it to the port you chose when you setup the Security lists at the start of this (my example was 4242)
In the domain line set it to grafana.$EXTERNAL_IP.nip.io (where $EXTERNAL_IP is the vm's public IP number)
Locate the cert_file and set it to /home/opc/smallstep/tls-grafana.crt
Locate the cert_key and set it to /home/opc/smallstep/tls-grafana.key

In the Security section
Locate the admin_user it will default to admin, but it's a good idea to change it to be something different (Ok it's security through obscurity, but it's still a good idea)
Locate the admin_password, YOU MUST change it to something different, especially if you have not changed the admin_user

Locate the cookie_secure and set it to true

In the Plugins section
Locate allow_loading_unsigned_plugins and set it to oracle-oracle-telemetry

###Install the plugins

Little unclear here, but it looks like the instructions could do with tidying up
mkdir -p ~/grafana/data/plugins/oracle-telemetry-streaming-main 

Then in the root of the git repo (e.g. the $HOME/oracle-telemetry-streaming) directory
cp -r . ~/grafana/data/plugins/oracle-telemetry-streaming-main

(basically you want everything that was downloaded by git PLUS all of the stuff that was created) 

###Start Grafana
Now you can start Grafana
cd ~/grafana/bin
./grafana-server

Get the URL to access it
echo  https://grafana.$EXTERNAL_IP.nip.io:4242/login
https://grafana.79.72.75.15.nip.io:4242/

Note that (currently at least) when adding metrics to the dashboard you can search for names using a dot separated snake_case format with a hierarchical namespace (e.g iot.normalizeddata.power_consumption) HOWEVER though the names are found when running the promql itself that will fail, so you will need to use a statement along the lines of {__name__="iot.normalized.power_consumption"}

For detail on Promql including how to focus on individual tags (and how to get labels based on those tags)

For metrics with multiple sources (e.g. indoor and outdoor temperature may both have a metric called iot.normalizeddata.temperature) grafana will display them all, but you can separate this out using object labels in the label field. For the current iot data upload set that might be something like {{iot_digital_twin_instance_display_name}}
