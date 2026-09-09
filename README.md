<div align="center">
  <h1>MaxProtect - Official Wiki</h1>
  <p>
    <img src="https://img.shields.io/badge/version-1.0.0-blue" alt="Version">
    <img src="https://img.shields.io/badge/Java-25+-red" alt="Java">
    <img src="https://img.shields.io/badge/Paper--Folia-1.21--26.1.2+-green" alt="Paper-Folia">
    <img src="https://img.shields.io/badge/Languages-EN_|_ES-blue" alt="Languages">
  </p>
</div>

Welcome to the official **MaxProtect** documentation. Here you will find everything you need to install, configure, and manage protections on your Minecraft server efficiently.

## Table of Contents

1. [Main Features](#1-main-features)
2. [Installation Guide](#2-installation-guide)
3. [Configuration Files](#3-configuration-files)
4. [Commands List](#4-commands-list)
5. [Permission Nodes](#5-permission-nodes)
6. [Protection Flags](#6-protection-flags)
7. [Modules and Systems Details](#7-modules-and-systems-details)

---

## 1. Main Features

**MaxProtect** is a modern and robust plugin designed to manage protection areas using specific blocks (Protection Stones). It is built with performance and customization in mind.

*   **Custom Protection Stones**: Create different protection tiers by defining specific blocks, radii, and prices.
*   **Advanced GUI Management**: Clean, interactive menus for creation, member management, flag configuration, and viewing logs.
*   **Vault Integration**: Full economy support for purchasing protections or paying rent.
*   **Rent System**: Expires and suspends protections if maintenance fees are not paid on time.
*   **Logs and Audits**: Tracks activity inside the protection (invitations, intrusions, ownership transfers).
*   **Region Flight (Fly)**: Allows players to automatically fly when entering a protection where they have permissions.
*   **Homes Menus**: Fast travel system to reach different regions owned by a player or where they are members.
*   **Visuals and Cinematics**: Custom particles, actionbars, and previews when placing or inspecting stones.
*   **Optimized for 1.21 and Folia**: Fully compatible with the latest versions and multi-threaded servers like Folia.

---

## 2. Installation Guide

### Dependencies
For MaxProtect to work correctly, you need to have the following plugins installed on your server:
*   **Required:** [WorldGuard](https://dev.bukkit.org/projects/worldguard) (Region management)
*   *Recommended:* [Vault](https://www.spigotmc.org/resources/vault.34315/) (For economy support and block purchasing)
*   *Optional:* ProtectionStones (For data migration systems if you are moving from this plugin)

### Installation Steps
1.  Make sure your server is running **PaperMC 1.21** (or a compatible fork like Purpur or Folia) and you have **Java 21**.
2.  Download the **MaxProtect** `.jar` file.
3.  Place the `.jar` into your server's `plugins/` folder.
4.  Ensure you also have the required dependency (`WorldGuard`) in the same folder.
5.  Start or restart the server.
6.  (Optional) Navigate to the generated `plugins/MaxProtect/` folder to modify `config.yml` and create your protection blocks.

---

## 3. Configuration Files

When you start the plugin for the first time, several key files and folders will be generated:

*   `config.yml`: The main file where you can customize messages, prefixes, particles, sounds, group limits, GUI menus (Info, Settings, Logs, Members), and all enabled flags.
*   `protections/` (Folder): The individual configuration of each protection stone will be saved here (e.g., `spawn_guard.yml`, `vip_guard.yml`).
*   `lang/` (Folder): If you apply other language templates (`/mp lang`), they will be saved here. The plugin includes full default configurations for English (`en`) and Spanish (`es`).
*   `database.db`: Local SQLite database to log events, track rentals, and homes.

> **Tip:** The plugin offers an in-game command to visually generate protection blocks and their YAMLs automatically without having to manually touch the `protections/` folder: `/mp create`.

---

## 4. Commands List

The plugin has two main commands: `/maxprotect` (alias `/mp`) for administrative purposes, and `/protection` (alias `/p`) for player tools.

### Admin Commands (`/mp`)
| Command | Description | Required Permission |
| :--- | :--- | :--- |
| `/mp help` | Shows the admin commands help page. | `maxprotect.admin` |
| `/mp give <player> <stone_id> [amount]` | Gives protection stones to a player. | `maxprotect.protection.give` |
| `/mp create` | Opens the interactive menu to create a new stone. | `maxprotect.admin` |
| `/mp listplaced` | Menu to view and teleport to placed protections (admin mode). | `maxprotect.admin.listplaced` |
| `/mp lang` | Opens the menu to change the base language (templates). | `maxprotect.admin` |
| `/mp debug` | Inspects data state and issues with WorldGuard. | `maxprotect.admin.debug` |
| `/mp repair` | Repairs and cleans orphan protections in the database. | `maxprotect.admin.repair` |
| `/mp reload` | Reloads configurations and stones. | `maxprotect.admin` |
| `/mp migrate` | Migrates data from older plugins (like ProtectionStones). | `maxprotect.admin.migrate` |

### Player Commands (`/p`)
| Command | Description | Required Permission |
| :--- | :--- | :--- |
| `/p help` | Shows the player commands help page. | `maxprotect.protection` |
| `/p info` | Shows information about the current region. | `maxprotect.protection` |
| `/p menu` | Opens the main protection menu. | `maxprotect.protection` |
| `/p members` | Opens the menu to manage members. | `maxprotect.protection` |
| `/p settings` | Opens the menu to modify flags (PvP, Mobs, etc.). | `maxprotect.flags` |
| `/p home` | Opens the home selector to travel to your protections. | `maxprotect.protection` |
| `/p buy <stone_id> [amount]` | Buys a protection stone using Vault economy. | `maxprotect.protection.buy` |
| `/p fly` | Toggles flight if you are inside your protection. | `maxprotect.protection.fly` |
| `/p invite <accept/deny> <id>` | Accepts or denies a protection invite. | `maxprotect.protection` |
| `/p member add <player>` | Sends an invitation to add a member. | `maxprotect.protection` |

---

## 5. Permission Nodes

Below are all the permission nodes handled by the system:

### General
*   `maxprotect.protection`: Basic access to player commands, menus, etc. (Default: *True*)
*   `maxprotect.protection.buy`: Allows buying stones using Vault. (Default: *True*)
*   `maxprotect.flags`: Allows modifying protection flags from the GUI. (Default: *True*)

### Protection Limits
To use the configured groups in `config.yml` (`limits.groups` section), grant permissions such as:
*   `maxprotect.limits.vip`: Assigns radius and protection amount limits for the VIP group.
*   `maxprotect.limits.mvp`: Assigns limits for the MVP group (these groups are defined in the config).

### Administration (Staff)
*   `maxprotect.admin`: Full access to the plugin and administrative commands. (Default: *OP*)
*   `maxprotect.admin.debug`: Allows inspecting placed protections and viewing discrepancies. (Default: *OP*)
*   `maxprotect.admin.listplaced`: Allows opening the menu that lists all stones on the server. (Default: *OP*)
*   `maxprotect.admin.repair`: Allows repairing and cleaning broken records. (Default: *OP*)
*   `maxprotect.admin.logs`: Allows viewing the recent event history of any protection. (Default: *OP*)
*   `maxprotect.admin.report`: Allows generating database reports. (Default: *OP*)
*   `maxprotect.admin.migrate`: Allows previewing and applying migrations from other plugins. (Default: *OP*)
*   `maxprotect.protection.give`: Allows giving stones to players. (Default: *OP*)
*   `maxprotect.protection.fly`: Allows using the `/p fly` command. (Default: *OP*)
*   `maxprotect.protection.remove.others`: Allows removing/breaking protections that belong to other people. (Default: *OP*)

---

## 6. Protection Flags

The MaxProtect settings menu (`/p settings`) provides an intuitive UI to control dozens of internal WorldGuard region permissions, without requiring players to learn complex commands.

Notable flags include:
*   **Combat:** Player PvP, Mob PvP.
*   **Access:** Allow Entry, Doors and Trapdoors, Pressure Plates.
*   **Building:** Break Blocks, Place Blocks, Redstone Mechanisms (Buttons/Levers).
*   **Inventories:** Open Storage (Chests/Barrels), Use Workstations/Anvils.
*   **World:** TNT Explosions, Leaf Decay.
*   **Survival:** Keep Inventory, Keep Experience, Fall Damage, Potion Splash, Hunger Drain.

These flags apply states such as *Nobody*, *Members+* or *Everyone* based on the chosen configuration in the interface.

---

## 7. Modules and Systems Details

### Member Invitation System
Unlike other plugins where commands add a person instantly, MaxProtect uses a secure invitation system.
1. The owner uses `/p member add <player>`.
2. The user receives an interactive, clickable chat message where they can Choose to Accept or Deny.
3. If the user accepts, they are added to the WorldGuard region automatically and the owner receives a notification.
This entire process is comprehensively tracked in the **Protection Logs**.

### Logs System
Every protection keeps a recent history tracking key events:
*   `INVITE_CREATED`, `INVITE_ACCEPTED`, `INVITE_DENIED`
*   `MEMBER_REMOVED`
*   `OWNER_TRANSFERRED`
*   `INTRUSION_ALERT`

Staff and owners can audit what happened and when from an interactive GUI menu.

### Rent System
You can enable period-based payments (Rentals) from `config.yml`. If the rent system is turned on, protections that haven't been paid will be "suspended." The check is done via an asynchronous timer, and players will receive constant warnings if the period is close to expiring.

### Cinematic Interface
The visual system includes options like "Particles on Break", "Exclusive Purchase Sounds", and even a "Cinematic" module. When the cinematic system is enabled, upon trying to place or view a stone, the user can experience fake borders made of blocks and an adapted camera for several seconds.
