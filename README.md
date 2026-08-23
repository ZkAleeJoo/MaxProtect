# MaxProtections

MaxProtections is a modern Paper protection plugin based on WorldGuard regions, configurable protection stones, interactive menus, member ranks, multilingual files, and safe runtime tracking.

## Requirements

- Java 21 or newer.
- Paper API 1.21 compatible server.
- Spigot is not supported because the plugin uses Paper-only APIs.
- WorldGuard is required.
- Vault is optional and only needed for protection prices, economy purchases, and protection rent.
- ProtectionStones is optional and only needed while running `/mp migrate protectionstones`.

## Main Commands

Root command: `/maxprotections` with alias `/mp`.

| Command | Permission | Description |
| --- | --- | --- |
| `/mp create` | `maxprotections.admin` | Opens the admin protection-stone creation menu. |
| `/mp reload` | `maxprotections.admin` | Reloads config and language files. |
| `/mp lang` | `maxprotections.admin` | Opens the language selection menu. |
| `/mp debug <region>` | `maxprotections.admin.debug` | Prints owner, members, YAML state, WorldGuard state, bounds, and stone coordinates for a placed protection or WG region. |
| `/mp listplaced` | `maxprotections.admin.listplaced` | Opens the admin menu with every tracked placed protection. Click an entry to print debug details. |
| `/mp report protections` | `maxprotections.admin.report` | Prints SQL-backed totals, active/orphaned counts, largest regions, top owners, and logged event count. |
| `/mp logs <region-or-alias>` | `maxprotections.admin.logs` | Shows the latest stored events for any tracked protection. |
| `/mp repair` | `maxprotections.admin.repair` | Repairs missing SQL tracking for safe MaxProtections WorldGuard regions when the protection YAML and center stone can be identified. |
| `/mp repair cleanup` | `maxprotections.admin.repair` | Removes stale tracker entries whose WorldGuard region no longer exists. It does not delete WorldGuard regions. |
| `/mp migrate protectionstones preview` | `maxprotections.admin.migrate` | Scans ProtectionStones WorldGuard regions and reports what MaxProtections can import without changing data. |
| `/mp migrate protectionstones apply` | `maxprotections.admin.migrate` | Imports supported ProtectionStones regions into `protections.db` while keeping original region IDs, owners, members, bounds, and physical stone materials. |

## Protection Commands

Root command: `/protection` with alias `/p`.

| Command | Permission | Description |
| --- | --- | --- |
| `/p give <player> <protection> <amount>` | `maxprotections.protection.give` | Gives protection stones to a player. |
| `/p buy <protection>` | `maxprotections.protection.buy` | Buys one protection stone using Vault when a price is configured. |
| `/p rent [region]` | `maxprotections.protection` + protection owner | Pays overdue rent for the current or selected protection and reactivates it if suspended. |
| `/p fly` | `maxprotections.protection.fly` | Toggles flight while you remain inside an accessible protection. Flight is removed when leaving, dying, disconnecting, changing world, or losing access. |
| `/p menu` | Protection owner/member-admin rank or `maxprotections.admin` | Opens the protection menu for the region at your position. |
| `/p settings` | `maxprotections.flags` + protection owner/member-admin rank, or `maxprotections.admin` | Opens flag controls for the current protection. |
| `/p member` or `/p members` | Protection owner/member-admin rank or `maxprotections.admin` | Opens the member management menu. |
| `/p member add <player>` | Protection owner/member-admin rank or `maxprotections.admin` | Sends a pending invitation to an online player. |
| `/p member remove <player>` | Protection owner/member-admin rank or `maxprotections.admin` | Removes a member, teleports them outside if needed, and logs the event. |
| `/p invite accept <id>` | Invited player | Accepts a pending protection invitation. |
| `/p invite deny <id>` | Invited player | Rejects a pending protection invitation. |
| `/p trust <player>` | Protection owner/member-admin rank or `maxprotections.admin` | Shortcut for sending a member invitation. |
| `/p untrust <player>` | Protection owner/member-admin rank or `maxprotections.admin` | Shortcut for removing a member. |
| `/p transfer <player>` | Current protection owner | Transfers the current protection to another known player. The previous owner is kept as a protection-admin member. |
| `/p logs [id]` | Protection access/rank | Shows recent stored events for the current or selected accessible protection. |
| `/p teleport <id>` or `/p tp <id>` | Protection access/rank | Teleports to an accessible protection home. |
| `/p home` or `/p homes` | Protection access/rank | Opens the protection homes menu. |
| `/p sethome` | Protection owner | Sets the current protection home to your current location. |
| `/p alias <alias>` | Protection owner/member-admin rank or `maxprotections.admin` | Sets a friendly alias for the current protection. |
| `/p info [id]` | Current protection, or protection access/rank when using an id or alias | Shows information about the current or selected protection. |
| `/p view` | Any player inside a protection | Shows the protection border effect. |
| `/p leave` | Protection member/admin-member inside a protection | Removes your member access and teleports you outside the current protection. Owners cannot leave their own protection. |
| `/p list` | Player-owned protections | Lists protections owned by the player. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `maxprotections.admin` | OP | Full MaxProtections admin access. Includes admin debug/list/repair/report/migrate and protection child permissions. |
| `maxprotections.admin.debug` | OP | Allows `/mp debug <region>`. |
| `maxprotections.admin.listplaced` | OP | Allows `/mp listplaced`. |
| `maxprotections.admin.repair` | OP | Allows `/mp repair` and `/mp repair cleanup`. |
| `maxprotections.admin.report` | OP | Allows `/mp report protections`. |
| `maxprotections.admin.logs` | OP | Allows `/mp logs <region-or-alias>` for any tracked protection. |
| `maxprotections.admin.migrate` | OP | Allows previewing and applying supported protection-plugin migrations. |
| `maxprotections.protection` | Everyone | Allows player protection commands that require a base permission, including `/p rent`. |
| `maxprotections.protection.give` | OP | Allows `/p give`. |
| `maxprotections.protection.buy` | Everyone | Allows `/p buy`. |
| `maxprotections.protection.fly` | OP | Allows `/p fly` inside accessible protections. |
| `maxprotections.protection.remove.others` | OP | Allows removing another player's protection stone. |
| `maxprotections.flags` | Everyone | Allows protection owners/admins to modify protection flags from the menu. |

Protection limit permissions are generated dynamically from `limits.groups` in `config.yml`. For example, a group key `vip` defaults to `maxprotections.limits.vip` unless the group has a custom `permission`.

## Permission Setup Guide

The plugin uses two different access layers:

- Bukkit permissions decide who can use global features such as buying stones, paying rent, flying in protections, giving stones, removing other players' stones, and admin tools.
- Protection ranks decide what a player can do inside a specific placed protection. Owners and protection-admin members can manage members, aliases, menus, and settings; regular members only get access according to the protection flags.

### Normal Players

For a normal survival player, keep these permissions enabled:

| Permission | Why normal players need it |
| --- | --- |
| `maxprotections.protection` | Lets the player use base protection features that require the plugin permission, currently `/p rent`. |
| `maxprotections.protection.buy` | Lets the player buy protection stones with `/p buy <protection>`. |
| `maxprotections.flags` | Lets owners and protection-admin members edit flags from `/p settings`. Without this, owning the protection is not enough to change flags. |

These three permissions are `default: true` in `plugin.yml`, so regular players receive them automatically unless your permission plugin explicitly denies them. If your server uses a strict LuckPerms setup, you can still set them manually:

```text
/lp group default permission set maxprotections.protection true
/lp group default permission set maxprotections.protection.buy true
/lp group default permission set maxprotections.flags true
```

Normal players do not need extra permissions to place a valid protection stone, open their own protection menu, invite members, accept invites, teleport to accessible homes, set their own protection home, list their protections, view borders, or leave a protection. Those actions are controlled by ownership, invitations, member rank, and whether the player is inside or has access to that protection.

Protection owners can also use `/p transfer <player>` without an extra Bukkit permission. The command only works while standing inside the owned protection, transfers the WorldGuard and SQL owner to the target player, removes the new owner from the member list if needed, keeps the previous owner as a protection-admin member, and records the transfer in the protection logs.

### Rank Permissions

Protection limits come from `limits` in `config.yml`. The default config gives everyone the `default` profile unless they have a better group permission:

| Rank/group | Permission | Default limits |
| --- | --- | --- |
| Default | No extra permission | 3 protections, max radius 25, price range 0-100000. |
| VIP | `maxprotections.limits.vip` | 10 protections, max radius 50, price range 0-250000. |
| MVP | `maxprotections.limits.mvp` | 20 protections, max radius 75, price range 0-500000. |

If a player has multiple limit permissions, the group with the highest `priority` wins. In the default config, `mvp` has priority `20` and wins over `vip` priority `10`.

Example LuckPerms setup:

```text
/lp group vip permission set maxprotections.limits.vip true
/lp group mvp permission set maxprotections.limits.mvp true
```

You can add more ranks under `limits.groups` in `config.yml`. Each group can define its own `permission`, `priority`, `max-protections`, `max-radius`, `min-price`, and `max-price`. After changing limits, reload the plugin with `/mp reload`.

### Optional Extra Permissions

Use these only for selected ranks or staff:

| Permission | Recommended for | Notes |
| --- | --- | --- |
| `maxprotections.protection.fly` | VIP, MVP, staff | Lets the player use `/p fly`, but only inside protections they can access. Flight is removed when leaving, dying, disconnecting, changing world, or losing access. |
| `maxprotections.protection.give` | Staff, crates/rewards operators | Lets the sender use `/p give <player> <protection> <amount>`. Keep this away from normal players because it creates protection items. |
| `maxprotections.protection.remove.others` | Senior staff only | Lets staff remove another player's protection stone. This can affect player property, so treat it as a moderation/admin permission. |
| `maxprotections.admin.debug` | Support staff | Allows `/mp debug <region>` for diagnosing a placed protection or WorldGuard region. |
| `maxprotections.admin.listplaced` | Support staff | Allows `/mp listplaced`, the admin list of tracked protections. |
| `maxprotections.admin.report` | Admin staff | Allows `/mp report protections`, useful for auditing totals and database state. |
| `maxprotections.admin.logs` | Support/admin staff | Allows staff to inspect recent event logs for any tracked protection. |
| `maxprotections.admin.repair` | Owner/admin only | Allows `/mp repair` and cleanup. This changes tracking data and should not be given widely. |
| `maxprotections.admin.migrate` | Owner/admin only | Allows `/mp migrate protectionstones preview` and `/mp migrate protectionstones apply`. This imports external protection state and should not be given widely. |
| `maxprotections.admin` | Owner/admin only | Full plugin admin access. It includes admin debug/list/repair/report/migrate plus protection child permissions. |

### Suggested Server Setup

For a standard server:

| Server rank | Suggested permissions |
| --- | --- |
| Default | `maxprotections.protection`, `maxprotections.protection.buy`, `maxprotections.flags` |
| VIP | Default permissions + `maxprotections.limits.vip` + optional `maxprotections.protection.fly` |
| MVP | Default permissions + `maxprotections.limits.mvp` + optional `maxprotections.protection.fly` |
| Helper/mod | Default permissions + `maxprotections.admin.debug` + `maxprotections.admin.listplaced` + optional `maxprotections.admin.logs` |
| Admin | `maxprotections.admin` |
| Owner | `maxprotections.admin` + OP if you want full server-level control |

Test the setup in game with a non-OP account. Check `/p buy`, placing a stone, `/p settings`, `/p member add`, `/p home`, `/p logs`, `/p transfer` from the owner account, `/p rent` if rent is enabled, and verify that restricted commands such as `/p give`, `/p fly`, `/mp logs`, `/mp repair`, and breaking another player's protection stone are blocked unless the rank is supposed to have them.

## Runtime Files

- `protections/<id>.yml`: editable source for each protection stone, item, radius, price, WorldGuard defaults, and actionbar text.
- `protections.db`: SQLite runtime database for placed stones, owner/member state, pending invitations, event logs, aliases, homes, flags, rent status, region bounds, and stone coordinates.

Do not edit `protections.db` by hand while the server is running. Use the YAML files only for configurable templates, menus, language, and protection definitions. Use `/mp repair` only when WorldGuard and the SQL tracking data drift out of sync.

## ProtectionStones Migration

MaxProtections can import existing ProtectionStones regions so the server can stop depending on ProtectionStones afterward.

Run this first while ProtectionStones is still installed and enabled:

```text
/mp migrate protectionstones preview
```

Review the report and make backups of your WorldGuard `regions.yml` files plus `plugins/MaxProtections/protections.db`. Then run:

```text
/mp migrate protectionstones apply
```

Imported protections keep their original WorldGuard region IDs, owner UUIDs, member UUIDs, bounds, home when available, and physical protection block material. During `apply`, MaxProtections reuses an existing `protections/<id>.yml` with the same material and detected radius when possible; otherwise it creates one automatically, such as `ps_diamond_block_r25.yml`, with `price: 0` and `price-rent: 0`. The generated YAML ID is used as the MaxProtections type, while the original WorldGuard region ID stays unchanged.

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
