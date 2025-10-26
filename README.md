# ServerBank
Minecraft Server Bank Plugin.

This allows your server to has a bank, you can change the bank UUID in the config.yml to use any player as bank, it includes your own minecraft account.

By default, the Server's Account UUID is the Ofline UUID of the account "Server", BTW it's better for you to join in the server ONCE using an offline account called Server to register it's vault account and sync with other plugins.

If you are using a Online (mojang authenticated) server, just turn it off once and after the setup, turn it online again, it's real needed. (But if you prefer to use your original minecraft account as a Server Bank... It's your choice, I won't judge you)

Dependencies:

You obviously need to download and install Vault and an Economy Plugin in your server as well, I recommend using CMI or Essentials!

- CMI: https://www.spigotmc.org/resources/cmi-300-commands-insane-kits-portals-essentials-economy-mysql-sqlite-much-more.3742/
- Essentials: https://www.spigotmc.org/resources/essentialsx.9089/
- Vault: https://www.zrips.net/cmivault/

# Config

Change the Server UUID in the configuration to any user UUID or the server UUID you want.

Commands:

- /bank pay
- /bank collect (percent)
- /bank take
- /bank reload
