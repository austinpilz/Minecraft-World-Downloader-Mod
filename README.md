# Minecraft: Digital Archiving
TODO

# Target Server: MinecraftOnline.com
[MinecraftOnline.com](https://minecraftonline.com) is commonly referred to as the _oldest_ ~~anarchy server~~ Minecraft
server to still be running. Their wiki states the server was launched within an hour of Mojang releasing Minecraft multiplayer
publicly and making server jars available.

I highly recommend visiting their website and accompanying wiki. They contain a wealth of information on the server history,
staff, eras, organizations, mishaps, etc. It was a pleasure to get to read through, although it'd take you weeks of reading
on end to make it through everything.

## Why This Server?
First and foremost, I'm a digital historian and have a passion for archiving precious digital memories that could otherwise
be lost to time.

I played Minecraft when I was young, and it's what I credit my brother and I's strong relationship on. We spend hours a day
for years building on our [own server](https://pilzbros.com). 10+ years later and it's still something we do from time to
time.

I came across [SalC1's Video](https://www.youtube.com/watch?v=HrUX1XJkoU4) about MinecraftOnline.com, how it was the oldest
Minecraft server, and how the apparent ineffective administration have caused many issues in its operation. In an act of
selflessness and digital historianism, SalC1 donated thousands of dollars for the server to be moved to more modern hardware.
Unfortunately, the administration team wasted the entire funds on a test server that was never used. A huge chance to
revitalize a milestone for the community, squandered. MinecraftOnline.com documents two "invasions" as a result of SalC1's
videos, one in [2017](https://minecraftonline.com/wiki/SalC1_Invasion) and [another](https://minecraftonline.com/wiki/Double_Trouble_Invasion) 
in 2023.

Coincidentally, the 2023 invasion wiki post states that as of Feburary 2023 the server was planning to update to Minecraft
`1.19.3`. As of November 2024, it's still on `1.12.2`.

My goal is not to disparage the hard work of the administrators of the server. It's an impossible and thankless job to 
maintain a server and community of this magnitude in your spare time. Keeping the service free and accessible comes with
personal sacrifice. However, with that being said, my trust in their ability to forever maintain a historical gem like
this began to erode.

What happens if the server goes down one day and just doesn't come back up? There's no guarantee the world download would
be made available. Links for temporary world downloads throughout the wiki are broken. Imagine growing up building on this
server. You're one of the 306,000 unique players that spend hours/days/months/years building and growing up alongside
others. Imagine not being able to look back one day on what you built.

I have endless nostalgia for Minecraft and absolutely treasure the worlds I have from over the last 10 years. I know
thousands of people treasure their memories on the server. Backups should follow the 3-2-1 rule, where the 1 is an
off-site backup. 

I was about to create the unsanctioned off-site backup.

## Map Structure
![MinecraftOnline.com Map Structure](docs/map-structure.png)

The middle most tan colored ring, labeled `Freedonia (March 2011)` was the original world border since the server began
in Minecraft Beta 1.3 until it was expanded in 2013. The outermost red ring represents the current (2024) world border.

For this project, I was only concerned with capturing the original most map from 2011. Anything extra was simply a bonus.
Speaking purely from the density of builds, it appears the majority of growth on the server happened between 2011 and 2013,
with relatively little in the expansion areas. This could easily be untrue as I do not have the server data to validate
those claims.

You can view the map using the [Minecraft Online Map Viewer](https://minecraftonline.com/map/#/-3086/64/1181/-9/Freedonia%20-%20overworld/World).
The viewer came in extremely handy when setting goals for the project as it allowed me to know roughly what and where I
needed to capture. I frequently referred to it while my mod and world downloader were carving large chunks out, I wanted
to make sure I was only targeting `Freedonia` in the beginning.

## Server Challenges

- Of the three rules on the server, using a hacked client for flight was prohibited. This immediately tells you that you'll be doing some deep configuration to avoid detection, kicks, and bans.
- The server _still_ runs on `Minecraft 1.12.2`. That's pretty old. You're unlikely to find active mod/tooling support for it. Coincidentally, my [Friday the 13th Minecraft](https://f13mc.com) server is still on 1.12.2. I just couldn't be arsed to rewrite everything when 1.13 broke like, every major API.
- The physical server is old or sure feels like it. They don't publish the current TPS counts, but Sal1C videos routinely documented it below 10 TPS, making basic functions like riding Minecarts impractical.

## The Plan

### The Downloader
We can yeet ourselves around a map all we want, but the entire purpose of this project is to archive the world. For that,
we'll need to download it.

I used the [minecraft-world-downloader](https://github.com/mircokroon/minecraft-world-downloader), a standalone proxy
application that writes the chunks you're exploring in Minecraft to disk. Once you're finished, you can open that save
as a single-player world or host it on your own server. 

1. You run the world downloader as an application on your computer. It hosts itself on a port and authenticates itself with Microsoft/Mojang using your account.
2. In your Minecraft client (with your mods), you connect to the port defined in the previous step.
3. The world downloader becomes a proxy. It sits between you and your destination server. As you explore the server, it intercepts the chunks and stores them locally.
4. When you're done, you have the world downloaded. Some restrictions apply.

Perhaps the most important thing to call out here is that for the download to work, you have to explore every single chunk
you wish to download. At least at time of writing, there is no way to download a server's world (as a player) like you'd
download a single file from the internet. The process outlined here is saving chunks of the world as you go to hopefully
produce a full world save as long as you explore it all.

This is _rough_ for large maps. Similar efforts have been done on 2b2t. You can read about it on [Reddit](https://old.reddit.com//r/2b2t/comments/t287n9/1170gb_of_2b2t_256000%C2%B2_mapping_project_info/)
and download the 1.17TB spawn world download via [this torrent](https://cloud.daporkchop.net/minecraft/2b2t/torrents/2b2t_256k%c2%b2_spawn_download.torrent).
For this effort, they traveled 128 _THOUSAND_ blocks in each direction.

### The Translation Layer
You're not going to find any resources for making a client-side Fabric mod for Minecraft `1.12.2`. Instead, like me, you
should probably use the most modern client mod libraries available.

To allow my modern mod to work on the legacy server version, you can also install [ViaFabricPlus](https://github.com/ViaVersion/ViaFabricPlus)
in your client as a mod. If you're a server administrator you've likely used the Via teams' plugins before to allow clients
to play on the server with client versions that differ from that of the server. ViaFabricPlus is no exception in the magic.
It allows your _client_ to handle packet translation between the destination server version and the client.

This allowed me to run a Minecraft `1.21.3` client mod and connect to a Minecraft `1.12.2` server with minimal issues. The
destination server has no idea you're not on the version it's expecting, it's the perfect crime.

### The Client-side Mod
We have the ability to download worlds and our client is able to connect to _any_ server version - now what?

We need to create a client-side mod that:
- Allows us to enable/disable the archiving functionality.
- Automagically move us towards the next point where we need to turn, taking us in a clockwise spiral to visit _all_ chunks on the server.
- Overlap _somewhat_ with chunks we've already seen so we're sure we have them saved.
- Handle being kicked and automatically resuming where we left off.
- Hopefully doesn't siphon the little sanity we had left.

![Flight Pattern](docs/flight-pattern.png)

## Architecture
![Architecture](docs/architecture.png)


## Let's Do This

## Learnings
Mods mad, banned thrice

Shortcomings, alt-provider would work well to avoid bans but the world downloader _also_ needs auth


