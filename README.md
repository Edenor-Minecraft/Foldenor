<div align=center>
    <img src="./foldenor.png">
    <br /><br />
    <p>Fork of <a href="https://github.com/PaperMC/Folia">Folia</a> which adds secure seed and some useful patches. This project is originally aimed for <a href="https://edenor.ru/">Edenor</a> Minecraft server.</p>
</div>

## About Folia
[Folia overview](https://docs.papermc.io/folia/reference/overview)

[Folia region logic](https://docs.papermc.io/folia/reference/region-logic)

## Features
 - **Bukkit Plugins Support!** Just add folia-supported: true at the end of any plugin.yml.
   > ⚠️ Attention: Not all plugins can run on Foldenor without changing the plugin code!
 - **Various patches** blending from [other forks](https://github.com/Edenor-Minecraft/Foldenor?tab=readme-ov-file#credits).
 - ...

## Downloads

Pre-built Jars can be found in the [Releases Tab](https://github.com/Edenor-Minecraft/Foldenor/releases/)

## Building

Building a Paperclip JAR for distribution:

```bash
./gradlew applyAllPatches && ./gradlew createMojmapPaperclipJar
```

## About Edenor
[Website](https://edenor.ru/)

## Communication with the developer
[Telegram](https://t.me/AltronMaxX)

## License
The PATCHES-LICENSE file describes the license for api & server patches,
found in `./patches` and its subdirectories except when noted otherwise.

The fork is based off of PaperMC's fork example found [here](https://github.com/PaperMC/paperweight-examples).
As such, it contains modifications to it in this project, please see the repository for license information
of modified files.

## Credits
Thanks to these projects below. Foldenor just mix some of their patches together.<br>
If these excellent projects hadn't appeared, Foldenor wouldn't have become great.

- [Purpur](https://github.com/PurpurMC/Purpur)
- [Leaf](https://github.com/Winds-Studio/Leaf)
- [Pufferfish](https://github.com/pufferfish-gg/Pufferfish)
- [Gale](https://github.com/GaleMC/Gale)
- [ShreddedPaper](https://github.com/MultiPaper/ShreddedPaper)
- [Leaves](https://github.com/LeavesMC/Leaves)
- [Kaiiju](https://github.com/KaiijuMC/Kaiiju)
- [SparklyPaper](https://github.com/SparklyPower/SparklyPaper)
- [Mizi](https://github.com/Toffikk/Mizi)
- [Canvas](https://github.com/CraftCanvasMC/Canvas)
