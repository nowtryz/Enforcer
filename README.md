# Enforcer

When finished, this plugin will:
 - A firewall to prevent crack users to steal an account
 - Sync one Discord server's roles with group for all users
 - Assign different groups for twitch subs and followers

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

 - Maven installed
 - A Spigot server (or Bukkit/Paper either)
 - Have the [Vault](https://dev.bukkit.org/projects/vault) plugin in your plugin folder
 - An app and a Bot created on [Discord Dev Portal](https://discordapp.com/developers/applications)

### Installing

1. Build the plugin
    ```
    mvn clean install
    ```
1. Put the resulting JAR available in `target/Enforcer-VERSION-SNAPSHOT.jar` in your plugin folder
1. Create an App and a Bot on [Discord Dev Portal](https://discordapp.com/developers/applications)
1. Run the server
1. Edit `plugins/Enforcer/config.yml`
1. Run `enforcer reload` in the console

## Built With

* [Discord4J](https://github.com/Discord4J/Discord4J) - A fast, reactive Java wrapper for the official Discord Bot API.
* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Authors

* **Damien Djmb** - *Initial work* - [Nowtryz](https://github.com/Nowtryz)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

