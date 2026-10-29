# [TaCZ] No Enchant

[![Get it on Modrinth](https://img.shields.io/badge/Get%20it%20on-Modrinth-black?style=for-the-badge&logo=modrinth&logoColor=00AF5C)](https://modrinth.com/project/HUdMcTWv)
<a href="https://www.curseforge.com/minecraft/mc-mods/tacznoenchant">
  <img
    alt="Get it on CurseForge"
    src="https://img.shields.io/badge/Get%20it%20on-CurseForge-f16436?style=for-the-badge&logo=curseforge&logoColor=white"
  />
</a>

Blocks enchanted-book upgrades on TACZ guns in survival.

- Stops normal anvil + book enchanting on TACZ guns.
- Allows creative-mode enchanting.
- Optional `stripExistingEnchantments` join cleanup.
- Works in singleplayer and multiplayer.

> [!NOTE]
> TaCZ Guns do not accept enchantments in survival by design but, when combined with other mods like [TACZ: Durability](https://www.curseforge.com/minecraft/mc-mods/tacz-durability), players can use some enchantments which this mod is made to prevent this for who prefers.

## Config
```toml
stripExistingEnchantments = false
allowedGunIds = []
```

`stripExistingEnchantments` is off by default.
Warning: this is irreversible and should only be used for emergency cleanup or migration. It is also meant to remove already-enchanted TACZ guns that players may have obtained through alternative methods or during brief enchant attempts that temporarily flash before being reset.

## Exempt guns example
```toml
allowedGunIds = ["tacz:ak47"]
```
