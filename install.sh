#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mvn clean install -DskipTests -q \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true \
    -Dgpg.skip=true \
    -Denforcer.skip=true
mkdir -p ~/.local/lib/xl
cp xl-assembly/target/xl-assembly-*-SNAPSHOT.jar ~/.local/lib/xl/xl.jar 2>/dev/null || \
  cp xl-assembly/target/xl-assembly-*.jar ~/.local/lib/xl/xl.jar
mkdir -p ~/.local/bin
cat > ~/.local/bin/xl <<'EOF'
#!/usr/bin/env bash
exec java -jar ~/.local/lib/xl/xl.jar "$@"
EOF
chmod +x ~/.local/bin/xl
echo "Installed: $(which xl)"
