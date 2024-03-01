<div align=center>
    <img src="./foldenor.png">
    <br /><br />
    <p>Fork of <a href="https://github.com/PaperMC/Folia">Folia</a> which adds secure seed, comparability for paper plugins and some useful patches. This project is originally aimed for <a href="https://edenor.ru/">Edenor</a> Minecraft server.</p>
</div>

## About Folia
[Folia overview](https://docs.papermc.io/folia/reference/overview)

[Folia region logic](https://docs.papermc.io/folia/reference/region-logic)

## Foldenor api

# How to use

[Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)

[Maven](https://docs.github.com/ru/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package)

# Maven

Artifact Information:
    
    <dependency>
		<groupId>dev.edenor.foldenor</groupId>
		<artifactId>foldenor-api</artifactId>
		<version>1.20.4-R0.1-SNAPSHOT</version>
	</dependency>

# Gradle

Gradle Repo (for foldenor-api):
    
	maven {
        url = uri("https://maven.pkg.github.com/Edenor-Minecraft/Foldenor/")
        credentials {
            username = System.getenv("USERNAME")
            password = System.getenv("TOKEN")
        }
   }

Artifact Information:
    
    dependencies {
	    compileOnly 'dev.edenor.foldenor:foldenor-api:1.20.4-R0.1-SNAPSHOT'
	}

## About Edenor
[VK](https://vk.com/edenorrp)

[Discord](https://discord.gg/bC66Pwh)

[Telegram](https://t.me/edenorrp)

[Website](https://edenor.ru/)

## Communication with the developer
[VK](https://vk.com/altronmaxx)

[Discord](https://discord.com/users/324794944042565643)

[GitHub](https://github.com/AltronMaxX)

[Telegram](https://t.me/AltronMaxX)

## License
The PATCHES-LICENSE file describes the license for api & server patches,
found in `./patches` and its subdirectories except when noted otherwise.

The fork is based off of PaperMC's fork example found [here](https://github.com/PaperMC/paperweight-examples).
As such, it contains modifications to it in this project, please see the repository for license information
of modified files.


