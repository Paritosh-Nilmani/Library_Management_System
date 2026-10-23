#!/bin/sh
set -eu

port=10000
if printenv PORT >/dev/null 2>&1; then
    port=$(printenv PORT)
fi
case "$port" in
    *[!0-9]*)
        echo "PORT must be an integer between 1 and 65535." >&2
        exit 1
        ;;
esac
if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
    echo "PORT must be an integer between 1 and 65535." >&2
    exit 1
fi

catalina_home=/usr/local/tomcat
if printenv CATALINA_HOME >/dev/null 2>&1; then
    catalina_home=$(printenv CATALINA_HOME)
fi
server_xml="$catalina_home/conf/server.xml"
if ! grep -q 'port="8080"' "$server_xml"; then
    echo "Tomcat's default HTTP connector was not found in $server_xml." >&2
    exit 1
fi

sed -i "s/port=\"8080\"/port=\"$port\" address=\"0.0.0.0\"/" "$server_xml"
exec catalina.sh run