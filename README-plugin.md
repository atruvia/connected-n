Connect-n loads classes during startup via service-loader interface. So all classes implememting ``org.ase.fourwins.tournament.listener.TournamentListener`` 
(with correspodending META-INF/service file) are instanciated. 

If you like to extend connect-n by yourself you can do so just by adding connect-n's core.jar as a compile time dependency of your java project. 
When calling java runtime by yourself you can add your jar using the ``-cp`` argument. 
To be able to use the prebuild docker image the classpath is extended by ``/plugins`` so if you mount a host directory containing JARs those are inspected by the service-loader as well. 

```
# path to connect-n core containing the listener
# you can generate it by building connect-n using maven inside the connect-n repo
CORE=~/path/to/core/target/core-0.0.1-SNAPSHOT.jar 

if [ ! -e $CORE ]; then
        echo "please specify a valid path to connect-n-core.jar" 
        exit 1
fi

# create sources
mkdir -p src/foo/
cat >src/foo/Bar.java <<EOF
package foo;
import org.ase.fourwins.tournament.listener.TournamentListener;
public class Bar implements TournamentListener {
        public Bar() { System.out.println("Hello World!"); }
}
EOF

mkdir -p target/META-INF/services/
cat >target/META-INF/services/org.ase.fourwins.tournament.listener.TournamentListener <<EOF
foo.Bar
EOF

# compile and create jar
mkdir -p target/
javac -source 9 -target 9 -cp "$CORE" -d target/ src/foo/Bar.java 
jar -cf myplugin.jar -C target/ META-INF -C target/ foo/

# run docker container
docker run --rm -v$PWD:/plugins/ fiduciagad/fourwins-udp
```
