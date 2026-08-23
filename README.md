# ArgosProtect

ArgosProtect is a modern Paper protection plugin based on WorldGuard regions, configurable protection stones, interactive menus, member ranks, multilingual files, and safe runtime tracking.

## Requirements

- Java 21 or newer.
- Paper API 1.21 compatible server.
- Spigot is not supported because the plugin uses Paper-only APIs.
- WorldGuard is required.
- Vault is optional and only needed for protection prices, economy purchases, and protection rent.
- ProtectionStones is optional and only needed while running `/ap migrate protectionstones`.

## Main Commands

Root command: `/argosprotect` with alias `/ap`.

| Command | Permission | Description |
| --- | --- | --- |
| `/ap create` | `argosprotect.admin` | Opens the admin protection-stone creation menu. |
| `/ap reload` | `argosprotect.admin` | Reloads config and language files. |
| `/ap lang` | `argosprotect.admin` | Opens the language selection menu. |
| `/ap debug <region>` | `argosprotect.admin.debug` | Prints owner, members, YAML state, WorldGuard state, bounds, and stone coordinates for a placed protection or WG region. |
| `/ap listplaced` | `argosprotect.admin.listplaced` | Opens the admin menu with every tracked placed protection. Click an entry to print debug details. |
| `/ap report protections` | `argosprotect.admin.report` | Prints SQL-backed totals, active/orphaned counts, largest regions, top owners, and logged event count. |
| `/ap logs <region-or-alias>` | `argosprotect.admin.logs` | Shows the latest stored events for any tracked protection. |
| `/ap repair` | `argosprotect.admin.repair` | Repairs missing SQL tracking for safe ArgosProtect WorldGuard regions when the protection YAML and center stone can be identified. |
| `/ap repair cleanup` | `argosprotect.admin.repair` | Removes stale tracker entries whose WorldGuard region no longer exists. It does not delete WorldGuard regions. |
| `/ap migrate protectionstones preview` | `argosprotect.admin.migrate` | Scans ProtectionStones WorldGuard regions and reports what ArgosProtect can import without changing data. |
| `/ap migrate protectionstones apply` | `argosprotect.admin.migrate` | Imports supported ProtectionStones regions into `protections.db` while keeping original region IDs, owners, members, bounds, and physical stone materials. |

## Protection Commands

Root command: `/protection` with alias `/p`.

| Command | Permission | Description |
| --- | --- | --- |
| `/p give <player> <protection> <amount>` | `argosprotect.protection.give` | Gives protection stones to a player. |
| `/p buy <protection>` | `argosprotect.protection.buy` | Buys one protection stone using Vault when a price is configured. |
| `/p rent [region]` | `argosprotect.protection` + protection owner | Pays overdue rent for the current or selected protection and reactivates it if suspended. |
| `/p fly` | `argosprotect.protection.fly` | Toggles flight while you remain inside an accessible protection. Flight is removed when leaving, dying, disconnecting, changing world, or losing access. |
| `/p menu` | Protection owner/member-admin rank or `argosprotect.admin` | Opens the protection menu for the region at your position. |
| `/p settings` | `argosprotect.flags` + protection owner/member-admin rank, or `argosprotect.admin` | Opens flag controls for the current protection. |
| `/p member` or `/p members` | Protection owner/member-admin rank or `argosprotect.admin` | Opens the member management menu. |
| `/p member add <player>` | Protection owner/member-admin rank or `argosprotect.admin` | Sends a pending invitation to an online player. |
| `/p member remove <player>` | Protection owner/member-admin rank or `argosprotect.admin` | Removes a member, teleports them outside if needed, and logs the event. |
| `/p invite accept <id>` | Invited player | Accepts a pending protection invitation. |
| `/p invite deny <id>` | Invited player | Rejects a pending protection invitation. |
| `/p trust <player>` | Protection owner/member-admin rank or `argosprotect.admin` | Shortcut for sending a member invitation. |
| `/p untrust <player>` | Protection owner/member-admin rank or `argosprotect.admin` | Shortcut for removing a member. |
| `/p transfer <player>` | Current protection owner | Transfers the current protection to another known player. The previous owner is kept as a protection-admin member. |
| `/p logs [id]` | Protection access/rank | Shows recent stored events for the current or selected accessible protection. |
| `/p teleport <id>` or `/p tp <id>` | Protection access/rank | Teleports to an accessible protection home. |
| `/p home` or `/p homes` | Protection access/rank | Opens the protection homes menu. |
| `/p sethome` | Protection owner | Sets the current protection home to your current location. |
| `/p alias <alias>` | Protection owner/member-admin rank or `argosprotect.admin` | Sets a friendly alias for the current protection. |
| `/p info [id]` | Current protection, or protection access/rank when using an id or alias | Shows information about the current or selected protection. |
| `/p view` | Any player inside a protection | Shows the protection border effect. |
| `/p leave` | Protection member/admin-member inside a protection | Removes your member access and teleports you outside the current protection. Owners cannot leave their own protection. |
| `/p list` | Player-owned protections | Lists protections owned by the player. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `argosprotect.admin` | OP | Full ArgosProtect admin access. Includes admin debug/list/repair/report/migrate and protection child permissions. |
| `argosprotect.admin.debug` | OP | Allows `/ap debug <region>`. |
| `argosprotect.admin.listplaced` | OP | Allows `/ap listplaced`. |
| `argosprotect.admin.repair` | OP | Allows `/ap repair` and `/ap repair cleanup`. |
| `argosprotect.admin.report` | OP | Allows `/ap report protections`. |
| `argosprotect.admin.logs` | OP | Allows `/ap logs <region-or-alias>` for any tracked protection. |
| `argosprotect.admin.migrate` | OP | Allows previewing and applying supported protection-plugin migrations. |
| `argosprotect.protection` | Everyone | Allows player protection commands that require a base permission, including `/p rent`. |
| `argosprotect.protection.give` | OP | Allows `/p give`. |
| `argosprotect.protection.buy` | Everyone | Allows `/p buy`. |
| `argosprotect.protection.fly` | OP | Allows `/p fly` inside accessible protections. |
| `argosprotect.protection.remove.others` | OP | Allows removing another player's protection stone. |
| `argosprotect.flags` | Everyone | Allows protection owners/admins to modify protection flags from the menu. |

Protection limit permissions are generated dynamically from `limits.groups` in `config.yml`. For example, a group key `vip` defaults to `argosprotect.limits.vip` unless the group has a custom `permission`.

## Permission Setup Guide

The plugin uses two different access layers:

- Bukkit permissions decide who can use global features such as buying stones, paying rent, flying in protections, giving stones, removing other players' stones, and admin tools.
- Protection ranks decide what a player can do inside a specific placed protection. Owners and protection-admin members can manage members, aliases, menus, and settings; regular members only get access according to the protection flags.

### Normal Players

For a normal survival player, keep these permissions enabled:

| Permission | Why normal players need it |
| --- | --- |
| `argosprotect.protection` | Lets the player use base protection features that require the plugin permission, currently `/p rent`. |
| `argosprotect.protection.buy` | Lets the player buy protection stones with `/p buy <protection>`. |
| `argosprotect.flags` | Lets owners and protection-admin members edit flags from `/p settings`. Without this, owning the protection is not enough to change flags. |

These three permissions are `default: true` in `plugin.yml`, so regular players receive them automatically unless your permission plugin explicitly denies them. If your server uses a strict LuckPerms setup, you can still set them manually:

```text
/lp group default permission set argosprotect.protection true
/lp group default permission set argosprotect.protection.buy true
/lp group default permission set argosprotect.flags true
```

Normal players do not need extra permissions to place a valid protection stone, open their own protection menu, invite members, accept invites, teleport to accessible homes, set their own protection home, list their protections, view borders, or leave a protection. Those actions are controlled by ownership, invitations, member rank, and whether the player is inside or has access to that protection.

Protection owners can also use `/p transfer <player>` without an extra Bukkit permission. The command only works while standing inside the owned protection, transfers the WorldGuard and SQL owner to the target player, removes the new owner from the member list if needed, keeps the previous owner as a protection-admin member, and records the transfer in the protection logs.

### Rank Permissions

Protection limits come from `limits` in `config.yml`. The default config gives everyone the `default` profile unless they have a better group permission:

| Rank/group | Permission | Default limits |
| --- | --- | --- |
| Default | No extra permission | 3 protections, max radius 25, price range 0-100000. |
| VIP | `argosprotect.limits.vip` | 10 protections, max radius 50, price range 0-250000. |
| MVP | `argosprotect.limits.mvp` | 20 protections, max radius 75, price range 0-500000. |

If a player has multiple limit permissions, the group with the highest `priority` wins. In the default config, `mvp` has priority `20` and wins over `vip` priority `10`.

Example LuckPerms setup:

```text
/lp group vip permission set argosprotect.limits.vip true
/lp group mvp permission set argosprotect.limits.mvp true
```

You can add more ranks under `limits.groups` in `config.yml`. Each group can define its own `permission`, `priority`, `max-protections`, `max-radius`, `min-price`, and `max-price`. After changing limits, reload the plugin with `/ap reload`.

### Optional Extra Permissions

Use these only for selected ranks or staff:

| Permission | Recommended for | Notes |
| --- | --- | --- |
| `argosprotect.protection.fly` | VIP, MVP, staff | Lets the player use `/p fly`, but only inside protections they can access. Flight is removed when leaving, dying, disconnecting, changing world, or losing access. |
| `argosprotect.protection.give` | Staff, crates/rewards operators | Lets the sender use `/p give <player> <protection> <amount>`. Keep this away from normal players because it creates protection items. |
| `argosprotect.protection.remove.others` | Senior staff only | Lets staff remove another player's protection stone. This can affect player property, so treat it as a moderation/admin permission. |
| `argosprotect.admin.debug` | Support staff | Allows `/ap debug <region>` for diagnosing a placed protection or WorldGuard region. |
| `argosprotect.admin.listplaced` | Support staff | Allows `/ap listplaced`, the admin list of tracked protections. |
| `argosprotect.admin.report` | Admin staff | Allows `/ap report protections`, useful for auditing totals and database state. |
| `argosprotect.admin.logs` | Support/admin staff | Allows staff to inspect recent event logs for any tracked protection. |
| `argosprotect.admin.repair` | Owner/admin only | Allows `/ap repair` and cleanup. This changes tracking data and should not be given widely. |
| `argosprotect.admin.migrate` | Owner/admin only | Allows `/ap migrate protectionstones preview` and `/ap migrate protectionstones apply`. This imports external protection state and should not be given widely. |
| `argosprotect.admin` | Owner/admin only | Full plugin admin access. It includes admin debug/list/repair/report/migrate plus protection child permissions. |

### Suggested Server Setup

For a standard server:

| Server rank | Suggested permissions |
| --- | --- |
| Default | `argosprotect.protection`, `argosprotect.protection.buy`, `argosprotect.flags` |
| VIP | Default permissions + `argosprotect.limits.vip` + optional `argosprotect.protection.fly` |
| MVP | Default permissions + `argosprotect.limits.mvp` + optional `argosprotect.protection.fly` |
| Helper/mod | Default permissions + `argosprotect.admin.debug` + `argosprotect.admin.listplaced` + optional `argosprotect.admin.logs` |
| Admin | `argosprotect.admin` |
| Owner | `argosprotect.admin` + OP if you want full server-level control |

Test the setup in game with a non-OP account. Check `/p buy`, placing a stone, `/p settings`, `/p member add`, `/p home`, `/p logs`, `/p transfer` from the owner account, `/p rent` if rent is enabled, and verify that restricted commands such as `/p give`, `/p fly`, `/ap logs`, `/ap repair`, and breaking another player's protection stone are blocked unless the rank is supposed to have them.

## Runtime Files

- `protections/<id>.yml`: editable source for each protection stone, item, radius, price, WorldGuard defaults, and actionbar text.
- `protections.db`: SQLite runtime database for placed stones, owner/member state, pending invitations, event logs, aliases, homes, flags, rent status, region bounds, and stone coordinates.

Do not edit `protections.db` by hand while the server is running. Use the YAML files only for configurable templates, menus, language, and protection definitions. Use `/ap repair` only when WorldGuard and the SQL tracking data drift out of sync.

## ProtectionStones Migration

ArgosProtect can import existing ProtectionStones regions so the server can stop depending on ProtectionStones afterward.

Run this first while ProtectionStones is still installed and enabled:

```text
/ap migrate protectionstones preview
```

Review the report and make backups of your WorldGuard `regions.yml` files plus `plugins/ArgosProtect/protections.db`. Then run:

```text
/ap migrate protectionstones apply
```

Imported protections keep their original WorldGuard region IDs, owner UUIDs, member UUIDs, bounds, home when available, and physical protection block material. During `apply`, ArgosProtect reuses an existing `protections/<id>.yml` with the same material and detected radius when possible; otherwise it creates one automatically, such as `ps_diamond_block_r25.yml`, with `price: 0` and `price-rent: 0`. The generated YAML ID is used as the ArgosProtect type, while the original WorldGuard region ID stays unchanged.

## Protection Rent

Rent is controlled from `protection.rent` in `config.yml`:

```yaml
protection:
  rent:
    enabled: false
    period-hours: 168
    check-interval-minutes: 10
```

When enabled, each placed protection receives a paid-until timestamp. The plugin periodically tries to charge the owner through Vault using `price-rent` from that protection's `protections/<id>.yml` file, while `price` remains the one-time buy price. Existing files without `price-rent` fall back to `price`, then to legacy `economy.price`. If the owner cannot pay or economy is unavailable, the protection is safely suspended: the SQL record, members, alias, home, and stone remain, but the WorldGuard region is removed so the area no longer protects. The owner can run `/p rent <region-or-alias>` after getting funds to pay and reactivate the region.
