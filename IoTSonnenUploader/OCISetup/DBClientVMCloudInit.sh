#!/bin/bash
set -euxo pipefail

LOG_FILE=/var/log/db-client-cloud-init.log
exec > >(tee -a "$LOG_FILE") 2>&1

echo "Starting DB client VM bootstrap at $(date -Is)"

dnf clean all
dnf makecache -y

mapfile -t JAVA_PACKAGES < <(rpm -qa | grep -Ei '(^java-|^jdk-|^jre-|openjdk)' || true)
if (( ${#JAVA_PACKAGES[@]} > 0 ))
then
  dnf remove -y "${JAVA_PACKAGES[@]}"
fi

dnf install -y java-25-openjdk-devel git maven curl unzip

JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
SQLCL_HOME=/opt/sqlcl

SQLCL_ZIP=/tmp/sqlcl-latest.zip
curl --fail --location --show-error --output "$SQLCL_ZIP" https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip
rm -rf "$SQLCL_HOME" /opt/sqlcl-*
unzip -q "$SQLCL_ZIP" -d /opt
ln -sfn "$SQLCL_HOME/bin/sql" /usr/local/bin/sql
ln -sfn "$SQLCL_HOME/bin/sql" /usr/local/bin/sqlcl
chmod +x "$SQLCL_HOME/bin/sql"

SYSTEM_PATH="$JAVA_HOME/bin:$SQLCL_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

rm -f /etc/profile.d/java.sh /etc/profile.d/sqlcl.sh
cat >/etc/profile.d/db-client-tools.sh <<EOF
export JAVA_HOME=$JAVA_HOME
export SQLCL_HOME=$SQLCL_HOME
path_prepend_if_missing() {
  case ":\$PATH:" in
    *":\$1:"*) ;;
    *) PATH="\$1:\$PATH" ;;
  esac
}
path_prepend_if_missing "\$SQLCL_HOME/bin"
path_prepend_if_missing "\$JAVA_HOME/bin"
path_prepend_if_missing /usr/local/bin
path_prepend_if_missing /usr/bin
export PATH
EOF
chmod 0644 /etc/profile.d/db-client-tools.sh

cat >/etc/environment <<EOF
JAVA_HOME=$JAVA_HOME
SQLCL_HOME=$SQLCL_HOME
PATH=$SYSTEM_PATH
EOF

export JAVA_HOME SQLCL_HOME PATH="$SYSTEM_PATH"

rm -f "$SQLCL_ZIP"

java -version
javac -version
git --version
mvn --version
sql -version || true
sqlcl -version || true

echo "Completed DB client VM bootstrap at $(date -Is)"
