package com.example.aquatura.data


data class FishInfo(
    val index: Int,
    val name: String,
    val scientificName: String,
    val localNames: List<String>,
    val description: String,
    val maxSizeCm: Float,
    val maxWeightKg: Float?,
    val habitat: String,
    val diet: String,
    val funFacts: List<String>,
    val isEdible: Boolean,
    val isNative: Boolean
)


object FishInfoRepository {
    
    private val fishInfoMap: Map<Int, FishInfo> = mapOf(
        
        0 to FishInfo(
            index = 0,
            name = "Bangus",
            scientificName = "Chanos chanos",
            localNames = listOf("Milkfish", "Bangos", "Awa"),
            description = "Bangus is a highly significant fish in Philippine aquaculture and cuisine. It has a smooth, shiny silver body and is known for its firm, flavorful white meat, though it contains many small intramuscular bones.",
            maxSizeCm = 180f,
            maxWeightKg = 14f,
            habitat = "Found in tropical marine and brackish waters. It is extensively farmed in fish ponds and pens across regions like Pangasinan.",
            diet = "Primary feeder on algae, small invertebrates, and plankton.",
            funFacts = listOf(
                "Dagupan City is famously known as the 'Bangus Capital of the Philippines.'",
                "Bangus can tolerate a wide range of salinity, from fresh to salt water.",
                "A single adult Bangus can have over 200 small bones, leading to the popularity of 'boneless' bangus products."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        1 to FishInfo(
            index = 1,
            name = "Big Head Carp",
            scientificName = "Hypophthalmichthys nobilis",
            localNames = listOf("Bighead Carp", "Noble Fish"),
            description = "A large freshwater fish with a very big head that can be one-third of its body length. Popular for food in Asia.",
            maxSizeCm = 105f,
            maxWeightKg = 40f,
            habitat = "Lives in lakes and rivers with slow-moving water. Found in Laguna de Bay and other large lakes.",
            diet = "Eats tiny floating plants and animals called zooplankton.",
            funFacts = listOf(
                "Its head is so big because it has large gills to filter food from water.",
                "Originally from China, it was brought to the Philippines for aquaculture.",
                "It can eat up to 20% of its body weight in plankton each day!"
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        2 to FishInfo(
            index = 2,
            name = "Black Spotted Barb",
            scientificName = "Puntius binotatus",
            localNames = listOf("Pait", "Barbo"),
            description = "A small fish with black spots on its body. It is a hardy fish that can live in different water conditions.",
            maxSizeCm = 18f,
            maxWeightKg = null,
            habitat = "Found in streams and rivers in Luzon, Visayas, and Mindanao.",
            diet = "Eats algae, small insects, and plant materials.",
            funFacts = listOf(
                "This fish is a good sign of clean water in rivers.",
                "It is often used as bait fish by local fishermen.",
                "These fish travel in groups called schools for protection."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        3 to FishInfo(
            index = 3,
            name = "Catfish",
            scientificName = "Clarias batrachus",
            localNames = listOf("Hito", "Pantat", "Ito"),
            description = "A fish with whisker-like organs near its mouth called barbels. It has smooth, slippery skin without scales.",
            maxSizeCm = 55f,
            maxWeightKg = 1.2f,
            habitat = "Lives in muddy ponds, rice paddies, and slow rivers throughout the Philippines.",
            diet = "Eats almost anything - worms, insects, small fish, and even dead animals.",
            funFacts = listOf(
                "Catfish can breathe air and survive outside water for many hours!",
                "They use their whiskers (barbels) to taste and feel food in muddy water.",
                "In the Philippines, crispy fried hito is a popular street food."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        4 to FishInfo(
            index = 4,
            name = "Climbing Perch",
            scientificName = "Anabas testudineus",
            localNames = listOf("Martiniko", "Puyo", "Climbing Gourami"),
            description = "A tough little fish famous for its ability to survive on land and even climb up trees and rocks using its fins.",
            maxSizeCm = 25f,
            maxWeightKg = 0.4f,
            habitat = "Found in ponds, swamps, and rice paddies in Luzon and Mindanao.",
            diet = "Eats small fish, insects, and plant materials.",
            funFacts = listOf(
                "It can stay alive outside water for 6 to 10 hours!",
                "It uses its gill covers and fins to 'walk' on land.",
                "When ponds dry up, it burrows into mud or travels to find new water."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        5 to FishInfo(
            index = 5,
            name = "Fourfinger Threadfin",
            scientificName = "Eleutheronema tetradactylum",
            localNames = listOf("Mamali", "Kughan", "Threadfin Salmon"),
            description = "A long, silvery fish with four thread-like fins under its body. It is considered a premium food fish.",
            maxSizeCm = 200f,
            maxWeightKg = 145f,
            habitat = "Found in coastal waters and river mouths in Manila Bay and Visayan seas.",
            diet = "Hunts small fish and shrimp near the sandy bottom.",
            funFacts = listOf(
                "The four 'fingers' under its body are special fins used to find food in sand.",
                "It is one of the most expensive fish in Philippine markets.",
                "Young fish start as males but change to females when they grow bigger!"
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        6 to FishInfo(
            index = 6,
            name = "Freshwater Eel",
            scientificName = "Anguilla marmorata",
            localNames = listOf("Igat", "Palos", "Casili"),
            description = "A long, snake-like fish with a slimy body. It can travel over wet land at night to move between ponds.",
            maxSizeCm = 200f,
            maxWeightKg = 20f,
            habitat = "Lives in rivers, streams, and lakes in mountainous areas throughout the Philippines.",
            diet = "Hunts at night for fish, frogs, crabs, and worms.",
            funFacts = listOf(
                "Eels can absorb oxygen through their skin, so they can survive on land when it's wet.",
                "They travel thousands of kilometers to the ocean to lay eggs, then die.",
                "Filipino fishermen catch them using bamboo traps called 'bubo'."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        7 to FishInfo(
            index = 7,
            name = "Glass Perchlet",
            scientificName = "Ambassis interrupta",
            localNames = listOf("Langaray", "Buwan-buwan", "Glassfish"),
            description = "A tiny fish with a see-through body. You can actually see its bones and organs through its skin!",
            maxSizeCm = 7f,
            maxWeightKg = null,
            habitat = "Found in coastal rivers and brackish waters in Luzon and Visayas.",
            diet = "Eats tiny water animals and insect larvae.",
            funFacts = listOf(
                "Its transparent body helps it hide from bigger fish that want to eat it.",
                "People sometimes keep these as aquarium fish because they are so unusual.",
                "They travel in large groups of hundreds for safety."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        8 to FishInfo(
            index = 8,
            name = "Goby",
            scientificName = "Glossogobius giuris",
            localNames = listOf("Biya", "Pijanga", "Tank Goby"),
            description = "A small bottom-dwelling fish that likes to hide under rocks and in sandy areas.",
            maxSizeCm = 50f,
            maxWeightKg = null,
            habitat = "Common in rivers, lakes, and ponds throughout the Philippines.",
            diet = "Eats small crabs, shrimp, and insect larvae on the river bottom.",
            funFacts = listOf(
                "Goby fry (baby fish) are harvested and sold as 'ipon' - a delicacy in Ilocos!",
                "Some gobies can stick to rocks using their fused pelvic fins like a suction cup.",
                "They are one of the most important food fish for poor communities."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        9 to FishInfo(
            index = 9,
            name = "Gold Fish",
            scientificName = "Carassius auratus",
            localNames = listOf("Goldfish", "Isdang Ginto"),
            description = "A colorful ornamental fish that comes in many colors like orange, white, red, and black. Popular as a pet.",
            maxSizeCm = 45f,
            maxWeightKg = 2f,
            habitat = "Originally from China, now found in aquariums and ornamental ponds in the Philippines.",
            diet = "Eats fish flakes, algae, mosquito larvae, and small worms.",
            funFacts = listOf(
                "Goldfish can remember things for at least 3 months - not just 3 seconds!",
                "They can recognize their owner's face and will swim to the front of the tank.",
                "The oldest goldfish ever recorded lived 43 years!"
            ),
            isEdible = false,
            isNative = false
        ),
        
        
        10 to FishInfo(
            index = 10,
            name = "Gourami",
            scientificName = "Osphronemus goramy",
            localNames = listOf("Gurami", "Giant Gourami"),
            description = "A large, gentle freshwater fish with thick lips. It is often raised in fish ponds for food.",
            maxSizeCm = 70f,
            maxWeightKg = 9f,
            habitat = "Found in ponds and slow rivers. Commonly raised in Laguna and Bulacan.",
            diet = "Eats water plants, fruits, and vegetables. Easy to feed with kangkong leaves!",
            funFacts = listOf(
                "The male builds a floating bubble nest to protect the eggs.",
                "It can breathe air directly using a special organ called 'labyrinth.'",
                "Gourami can live up to 20 years in good conditions."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        11 to FishInfo(
            index = 11,
            name = "Grass Carp",
            scientificName = "Ctenopharyngodon idella",
            localNames = listOf("Karpang Damo", "White Amur"),
            description = "A large fish that mostly eats plants. It is used to control weeds in lakes and ponds.",
            maxSizeCm = 150f,
            maxWeightKg = 45f,
            habitat = "Introduced in Philippine lakes for weed control and fish farming.",
            diet = "Eats water plants, grass, and leaves. Can eat 40% of its body weight daily!",
            funFacts = listOf(
                "One grass carp can eat up to 40 kilograms of plants every day!",
                "It is used as 'natural lawnmower' to clear weeds from lakes.",
                "Originally from China, it cannot reproduce naturally in Philippine waters."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        12 to FishInfo(
            index = 12,
            name = "Green Spotted Puffer",
            scientificName = "Dichotomyctere nigroviridis",
            localNames = listOf("Butete", "Burirawan"),
            description = "A round fish covered with green spots that can puff up like a balloon when scared!",
            maxSizeCm = 17f,
            maxWeightKg = null,
            habitat = "Found in brackish river mouths and coastal areas.",
            diet = "Eats snails, clams, crabs, and other shellfish with its strong beak-like teeth.",
            funFacts = listOf(
                "It puffs up by swallowing water or air to look too big to eat!",
                "WARNING: Its organs contain deadly poison called tetrodotoxin.",
                "Its teeth never stop growing, so it must eat hard-shelled food to wear them down."
            ),
            isEdible = false,
            isNative = true
        ),
        
        
        13 to FishInfo(
            index = 13,
            name = "Indian Carp",
            scientificName = "Catla catla",
            localNames = listOf("Catla", "Karpa"),
            description = "A large fish with a big head and upturned mouth. One of the fastest growing freshwater fish.",
            maxSizeCm = 180f,
            maxWeightKg = 45f,
            habitat = "Raised in fish ponds in Central Luzon and other aquaculture areas.",
            diet = "Eats plankton and tiny organisms floating in water.",
            funFacts = listOf(
                "It can grow up to 2 kilograms in just one year!",
                "The 'Big Three' Indian carps (catla, rohu, mrigal) are top aquaculture fish in Asia.",
                "It feeds near the water surface while other carps feed at the bottom."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        14 to FishInfo(
            index = 14,
            name = "Indo-Pacific Tarpon",
            scientificName = "Megalops cyprinoides",
            localNames = listOf("Buanbunan", "Buan-buan", "Oxeye Tarpon"),
            description = "A silvery fish with very large, shiny scales. It is a popular game fish because it jumps when hooked!",
            maxSizeCm = 150f,
            maxWeightKg = 18f,
            habitat = "Found in coastal waters, estuaries, and river mouths throughout the Philippines.",
            diet = "Hunts small fish, shrimp, and crabs.",
            funFacts = listOf(
                "It can breathe air by gulping at the surface - you can hear them make a rolling sound!",
                "When hooked, it leaps high out of the water making it exciting for anglers.",
                "Its large, silvery scales were once used to make decorative items."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        15 to FishInfo(
            index = 15,
            name = "Jaguar Gapote",
            scientificName = "Parachromis managuensis",
            localNames = listOf("Jaguar Cichlid", "Aztec Cichlid", "Managua Cichlid"),
            description = "An aggressive fish with beautiful jaguar-like spots. Originally from Central America.",
            maxSizeCm = 55f,
            maxWeightKg = 1.5f,
            habitat = "An invasive species now found in Laguna de Bay and other lakes. Can survive in many conditions.",
            diet = "A fierce predator that eats other fish, shrimp, and sometimes small frogs.",
            funFacts = listOf(
                "It is considered an invasive pest that threatens native Philippine fish.",
                "Both parents guard their eggs and baby fish aggressively.",
                "It was likely introduced as a released aquarium fish."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        16 to FishInfo(
            index = 16,
            name = "Janitor Fish",
            scientificName = "Pterygoplichthys pardalis",
            localNames = listOf("Pleco", "Dahos", "Suckermouth Catfish"),
            description = "A fish covered with bony armor plates that uses its sucker mouth to clean algae from surfaces.",
            maxSizeCm = 50f,
            maxWeightKg = 0.8f,
            habitat = "Invasive in Laguna de Bay, Marikina River, and Pasig River.",
            diet = "Eats algae, dead plants, and leftover food on the river bottom.",
            funFacts = listOf(
                "It is an invasive species causing problems in Philippine rivers and lakes.",
                "Its bony armor makes it difficult for predators to eat.",
                "In the aquarium hobby, it's called 'pleco' and sold as a tank cleaner."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        17 to FishInfo(
            index = 17,
            name = "Knifefish",
            scientificName = "Chitala ornata",
            localNames = listOf("Knifefish", "Clown Knifefish", "Featherback"),
            description = "A flat, knife-shaped fish that swims by undulating its long fin like a wave.",
            maxSizeCm = 100f,
            maxWeightKg = 5f,
            habitat = "Found in slow rivers and lakes. Originally from Southeast Asia.",
            diet = "A nighttime hunter that eats small fish, shrimp, and insects.",
            funFacts = listOf(
                "It can swim forwards and backwards equally well!",
                "It makes electrical signals to sense prey in dark, murky water.",
                "In Thailand, its meat is pounded into fish balls - a popular street food."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        18 to FishInfo(
            index = 18,
            name = "Long-Snouted Pipefish",
            scientificName = "Syngnathus acus",
            localNames = listOf("Pipefish", "Isdang Tubo"),
            description = "A long, thin fish that looks like a swimming stick or pipe. It is related to the seahorse!",
            maxSizeCm = 47f,
            maxWeightKg = null,
            habitat = "Found in seagrass beds and coastal waters.",
            diet = "Sucks in tiny shrimp and plankton through its tube-like snout.",
            funFacts = listOf(
                "Just like seahorses, the male pipefish carries the eggs in a brood pouch!",
                "It swims upright and uses its tiny fins to hover in seagrass.",
                "Its body is covered in bony rings for protection."
            ),
            isEdible = false,
            isNative = true
        ),
        
        
        19 to FishInfo(
            index = 19,
            name = "Mosquito Fish",
            scientificName = "Gambusia affinis",
            localNames = listOf("Kataba", "Mosquitofish"),
            description = "A tiny fish introduced to the Philippines to eat mosquito larvae and fight malaria.",
            maxSizeCm = 7f,
            maxWeightKg = null,
            habitat = "Found in ponds, ditches, and rice paddies throughout the Philippines.",
            diet = "Eats mosquito larvae, tiny insects, and algae.",
            funFacts = listOf(
                "One mosquito fish can eat over 100 mosquito larvae in a day!",
                "Females give birth to live babies instead of laying eggs.",
                "Originally from North America, it was brought to fight mosquito-borne diseases."
            ),
            isEdible = false,
            isNative = false
        ),
        
        
        20 to FishInfo(
            index = 20,
            name = "Mudfish",
            scientificName = "Channa striata",
            localNames = listOf("Dalag", "Haluan", "Mudfish"),
            description = "A tough, powerful fish that can survive in muddy, low-oxygen water. It is a fierce predator.",
            maxSizeCm = 100f,
            maxWeightKg = 3f,
            habitat = "Common in rice paddies, ponds, and swamps throughout the Philippines.",
            diet = "Hunts fish, frogs, shrimp, and even small birds!",
            funFacts = listOf(
                "It can survive buried in mud for months during dry season!",
                "It can 'walk' across land using its pectoral fins to find new water.",
                "Dalag soup is a famous remedy for people recovering from surgery in the Philippines."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        21 to FishInfo(
            index = 21,
            name = "Mullet",
            scientificName = "Mugil cephalus",
            localNames = listOf("Banak", "Gisaw", "Flathead Mullet"),
            description = "A streamlined, fast-swimming fish that often leaps out of the water. Very popular as food fish.",
            maxSizeCm = 100f,
            maxWeightKg = 8f,
            habitat = "Found in coastal waters, estuaries, and brackish fish ponds.",
            diet = "Eats algae, tiny plants, and decaying organic matter from the bottom.",
            funFacts = listOf(
                "Its gizzard-like stomach can grind food like a bird's gizzard!",
                "Mullet roe (eggs) is considered a delicacy and can be very expensive.",
                "When startled, a whole school will leap out of the water at once!"
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        22 to FishInfo(
            index = 22,
            name = "Pangasius",
            scientificName = "Pangasianodon hypophthalmus",
            localNames = listOf("Cream Dory", "Swai", "Basa"),
            description = "A large catfish with smooth skin and white flesh. Very popular as affordable food fish.",
            maxSizeCm = 130f,
            maxWeightKg = 44f,
            habitat = "Commonly raised in fish ponds. Native to Mekong River in Vietnam.",
            diet = "Eats almost anything - fish food pellets, vegetables, and other fish.",
            funFacts = listOf(
                "It is sold as 'cream dory' in the Philippines, though it's not a real dory!",
                "One of the most farmed fish in the world after tilapia and carp.",
                "It can grow very fast - reaching 1 kilogram in just 6 months."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        23 to FishInfo(
            index = 23,
            name = "Perch",
            scientificName = "Leiopotherapon plumbeus",
            localNames = listOf("Ayungin", "Silver Perch"),
            description = "A small, silvery fish native to the Philippines. It is becoming rare due to pollution and invasive species.",
            maxSizeCm = 15f,
            maxWeightKg = null,
            habitat = "Endemic to Laguna de Bay and connected rivers. Getting harder to find.",
            diet = "Eats small insects, snails, and algae.",
            funFacts = listOf(
                "Ayungin is the subject of a famous Filipino story 'Ang Alamat ng Ayungin.'",
                "It is now considered 'Vulnerable' because its population has dropped sharply.",
                "Dried ayungin is a traditional delicacy called 'tuyo ng ayungin.'"
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        24 to FishInfo(
            index = 24,
            name = "Scat Fish",
            scientificName = "Scatophagus argus",
            localNames = listOf("Kitang", "Kapiged", "Spotted Scat"),
            description = "A round, flat fish with beautiful leopard-like spots. Often kept in aquariums.",
            maxSizeCm = 38f,
            maxWeightKg = null,
            habitat = "Found in brackish waters, mangroves, and coastal areas.",
            diet = "Eats almost anything including algae, scraps, and waste materials.",
            funFacts = listOf(
                "Its scientific name 'Scatophagus' means 'dung eater' - it cleans up waste in water!",
                "Young fish have more spots that fade as they grow older.",
                "It can live in both freshwater and saltwater."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        25 to FishInfo(
            index = 25,
            name = "Silver Barb",
            scientificName = "Barbonymus gonionotus",
            localNames = listOf("Tawes", "Java Barb"),
            description = "A medium-sized, silvery fish that is easy to raise and grows fast.",
            maxSizeCm = 40f,
            maxWeightKg = 1f,
            habitat = "Raised in fish ponds throughout the Philippines. Native to Southeast Asia.",
            diet = "Eats water plants, algae, and leftover rice bran.",
            funFacts = listOf(
                "It is used to control weeds in fish ponds because it eats so many plants.",
                "Popular in aquaculture because it can tolerate crowded conditions.",
                "In Thailand, it is one of the most popular fish for temple ponds."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        26 to FishInfo(
            index = 26,
            name = "Silver Carp",
            scientificName = "Hypophthalmichthys molitrix",
            localNames = listOf("Karpang Pilak", "Silver Carp"),
            description = "A large, silver fish that is famous for jumping high out of the water when startled by boat engines!",
            maxSizeCm = 105f,
            maxWeightKg = 50f,
            habitat = "Introduced in Philippine lakes for aquaculture.",
            diet = "Filters tiny algae and plankton from the water.",
            funFacts = listOf(
                "It can jump up to 3 meters high when scared by boat motors!",
                "In the USA, it's considered invasive and dangerous to boaters because of its jumping.",
                "Its filter-feeding helps clean the water of excess algae."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        27 to FishInfo(
            index = 27,
            name = "Silver Perch",
            scientificName = "Bidyanus bidyanus",
            localNames = listOf("Silver Perch", "Grunter"),
            description = "A silvery fish that makes grunting sounds by grinding its teeth together!",
            maxSizeCm = 50f,
            maxWeightKg = 8f,
            habitat = "Native to Australia, raised in some Philippine fish farms.",
            diet = "Eats insects, small crustaceans, and plant matter.",
            funFacts = listOf(
                "It makes 'grunting' sounds when caught or stressed!",
                "In Australia, it was once the most popular freshwater sport fish.",
                "It can live up to 26 years in the wild."
            ),
            isEdible = true,
            isNative = false
        ),
        
        
        28 to FishInfo(
            index = 28,
            name = "Snakehead",
            scientificName = "Channa micropeltes",
            localNames = listOf("Dalag Bato", "Giant Snakehead"),
            description = "A large, powerful predator with a head shaped like a snake. One of the most aggressive freshwater fish.",
            maxSizeCm = 130f,
            maxWeightKg = 20f,
            habitat = "Found in lakes, rivers, and reservoirs in Luzon and Mindanao.",
            diet = "Eats fish, frogs, birds, and even small mammals!",
            funFacts = listOf(
                "It can breathe air and survive on land for several days if kept moist!",
                "Parents fiercely guard their babies and will attack anything that comes close.",
                "Young fish are bright red-orange, but turn dark as they grow."
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        29 to FishInfo(
            index = 29,
            name = "Tenpounder",
            scientificName = "Elops machnata",
            localNames = listOf("Bidbid", "Ladyfish", "Ten-pounder"),
            description = "A long, silvery fish that fights hard when hooked. Popular among sport fishermen.",
            maxSizeCm = 100f,
            maxWeightKg = 5f,
            habitat = "Found in coastal waters, estuaries, and river mouths.",
            diet = "Hunts small fish and shrimp near the surface.",
            funFacts = listOf(
                "Despite its name, most tenpounder caught are around 1-2 kilograms.",
                "Its flesh is very bony, so many fishermen release them after catching.",
                "Baby tenpounder look completely different - flat and transparent like a leaf!"
            ),
            isEdible = true,
            isNative = true
        ),
        
        
        30 to FishInfo(
            index = 30,
            name = "Tilapia",
            scientificName = "Oreochromis niloticus",
            localNames = listOf("Tilapya", "St. Peter's Fish", "Nile Tilapia"),
            description = "One of the most popular food fish in the Philippines. Easy to raise and affordable for everyone.",
            maxSizeCm = 60f,
            maxWeightKg = 4f,
            habitat = "Raised in fish ponds, cages, and lakes throughout the Philippines.",
            diet = "Eats algae, plants, and commercial fish feed.",
            funFacts = listOf(
                "Tilapia is the second most farmed fish in the world after carp!",
                "Female tilapia carry their eggs and babies in their mouth to protect them.",
                "It was called 'St. Peter's Fish' because it's believed to be the fish in the Bible story."
            ),
            isEdible = true,
            isNative = false
        )
    )
    

    fun getByIndex(index: Int): FishInfo? = fishInfoMap[index]
    

    fun getByName(name: String): FishInfo? {
        return fishInfoMap.values.find { 
            it.name.equals(name, ignoreCase = true) ||
            it.localNames.any { local -> local.equals(name, ignoreCase = true) }
        }
    }
    

    fun getRandomFunFact(index: Int): String? {
        return fishInfoMap[index]?.funFacts?.randomOrNull()
    }
    

    fun getAll(): List<FishInfo> = fishInfoMap.values.toList()
    

    fun getNativeFish(): List<FishInfo> = fishInfoMap.values.filter { it.isNative }
    

    fun getEdibleFish(): List<FishInfo> = fishInfoMap.values.filter { it.isEdible }
}
