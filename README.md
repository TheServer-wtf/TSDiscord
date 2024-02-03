# TSDiscord plugin
**A Discord link plugin written for TheServer.wtf**
***
## Plugin information
### Commands:
- **/tsdiscord [reload/debug]**
  - `Main plugin command for TSDiscord`

### Permissions:
- **tsdiscord.admin**
  - default: op
  - `Permission to use the /tsdiscord command`

***
## Using plugin functions
### Maven repository
```xml
<repository>
  <id>pghost-repository-theserver</id>
  <url>https://repo.pghost.org/theserver/</url>
</repository>
```

### Dependency
```xml
<dependency>
    <groupId>hu.Pdani</groupId>
    <artifactId>TSDiscord-API</artifactId>
    <version>1.0.0</version> <!-- NOTE: The API version differs from the plugin version! Check the repo for the latest version. -->
    <scope>provided</scope>
</dependency>
```

