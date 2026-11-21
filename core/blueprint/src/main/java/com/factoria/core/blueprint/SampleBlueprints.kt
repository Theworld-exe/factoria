package com.factoria.core.blueprint

data class SampleBlueprintEntry(
    val id: String,
    val title: String,
    val description: String,
    val blueprintString: String
)

/**
 * Verified, zlib-compressed Factorio 2.0 blueprint strings used by the Blueprint Inspector
 * sample loader and the pre-made blueprint library.
 */
object SampleBlueprints {

    val SOLAR_STARTER = SampleBlueprintEntry(
        id = "solar-starter",
        title = "Solar Starter Stamp (4:1)",
        description = "4 Solar Panels, 1 Accumulator, and 1 Medium Electric Pole.",
        blueprintString = "0eNqVkk1rxCAQQP+KzKkFE/K5kNz20GNhYY9lKSYZiqBG1JSGkP9eY8q2tAnbPanDzJvnMBM0YkBtuHJQT8AdSqh/xCgI1qDwsXMvmCFnx4zDcEpNHoo6ffQ5HdrWcO14r3xmQdbcE1MoLCUpObbtIAfBXG8oYarzoWfs+CDJk8DWGd6SUy8w9qh3NDZgykNWFVVV5kWW5mlGgbe9slC/eEvV4QfUKQXL3xQTi7kbNfre4QMUFJPLyy4ekV48YJ7ptTK7Wcm+jX3lhQIqxx3HVSA8xlc1yAZNENloSEH3lq8zmcB3jdK4pDCul6Dzi5P9j3MLk9+ps4Mp7rPZoZSbI/1DSeIkUIo42aIcrhQZ9ibCr72JtN+bfVyy4C7z/AmsFvIS"
    )

    val QUAD_NUCLEAR = SampleBlueprintEntry(
        id = "quad-nuclear",
        title = "480MW Quad Nuclear Reactor Core",
        description = "2x2 Nuclear Reactor core (480MW with 200% neighbor bonus), heat-pipe manifolds, and substations.",
        blueprintString = "0eNqd1V1PgzAUBuC/0jQx0aRbSoE5duu1JnrjhVlMgePWBAqhRbcs++8WiM7pmrbe8XH6nLdwCAecVz20nZAarw5YaKjx6sc1giueQ2WuJUt6/4wee16ih76ogHfoCXihmw7dNR2YyhJU0YlWi0aaerZjfwoLU4iuJ+lD6C1ilF4hCWKzzc3tvJG9uiFoC1zPWtECqrkUb01VKoK4LJHqc6X50EDNTcN36NTYLF2wLMmyNE5YFEeMYFGYErx6MTuSJezwKiJYiY3k1bBLvW/BJBw3S7Dk9XAmp6yzbsqKj0fyvZo5V4+RYVdsudzAsHhNMEgttIApx3iyf5V9nZv7Qx5LX4LbRonpIR6waT5j85Tg/XQwpvplMX/LRcX/iGWhkvBUFik9f8rDYFyIE39tLb2sLIIUC3LrhTBHlGWQYkEyLyRyRIloEGNTIi+FusKwIMamxD6KM0sSotgQr9F1vqNFiGJDvEbXNbnRMkSxIV6j6/qeGQ1RbIjX4CauKCxEsSGnsT395i5M/2JOR4bO6UUm8WLOlPXx+AkvfJT6"
    )

    val GREEN_CIRCUIT_ARRAY = SampleBlueprintEntry(
        id = "green-circuit-array",
        title = "Green Circuit 3:2 Tileable Array",
        description = "Compact 3:2 Copper Cable to Electronic Circuit assembler block.",
        blueprintString = "0eNqlkttOxCAQhl+l4Zo2Pa5u70xjfAHvzMZQOq4TWyBAjc2m7y49pHHTNW7XO2Dg++YHTqSsW1AahSX5iaCFhuQ/1iipWQm1W3vSAMIrUPMWrZfksfeMNbCyBu9Ba9a5rRUYrlFZlMIdKGSjGJ+2FlIp0F4xbrfSe6yBWy0F8oXIjIHGlbVX1pJ/BI73CdqMrGwX79P9PkvSOEqimBLkUhiSv7iORQVfJI8oMXgUrB5S2E6Ba2AMQ4lgzTCDRenzSUn6ni6A+E8AHzP4fMjgjh4oAWHRIkyNjJPuVbRNCXpsaD43B0Nx9BvG31GAnziqkganmzoR5/eTIKOkc4MoyHpKNHBUay1dieKNojAIb/IkGz235km3Xlw0i+Jzz6X3XtuyjbZ/yXaL7I0Z66MwoK0rrDPFsyYcNRU60VROL2DvrsaGW7D312I3UffXUjddQRQu2AYqbBt/ehH3Hkq6f/b7rxnwl4DRRuAZ79D33wk7w84="
    )

    val KOVAREX_ENRICHMENT = SampleBlueprintEntry(
        id = "kovarex-enrichment",
        title = "Kovarex Enrichment Loop",
        description = "Self-sustaining centrifuge Kovarex loop with productivity modules and filtered recirculation.",
        blueprintString = "0eNqNks1uwjAQhF/F8tlGxCFIcO+pvfVYVZXjLLDCcSL/UBDKu3eTtCkqtOWU2N79dmbsMy9tgtaji3x95hih5uuLPcGtLsHS3mNz0B6O7MF5NLsaXGRPTdNSRQXBeGwjNo7qnsFuZEghanTotsxQpcdN2gL7QljqY+8Yd6z1TZVMxAPGE6vp30Jg2lVsgzaCh4p5MOhNsrrHz2jaAXwYJhVLtVqsVkW+UFmeKcHRNC7w9QvZcBUc+ToTPODWadtbi6cWSN7gUHCn636VvHaYaqnygnedmDrVv53ftqjxVXBaYkQY5w+L05tLdQl+0HHdJXjbBBxDO3OaOZ/NBT8N307w3vcwdj+GJmHKXVJqBkIgRK8p9P2XQcoxSJmTj8HUDzlqklMmu5foAngK+1qRVJ+SZDZoqpBUjefqBje/l/s7dnkDu5iwGx3iHdi7xBYTNUQAK80OQrxm5hfMG5TlRKmh6h8SWBpKFyXbxv5xx2pW0Kvpug+mKCym"
    )

    val SPACE_PLATFORM_HUB = SampleBlueprintEntry(
        id = "space-platform-hub",
        title = "Space Platform Starter Hub",
        description = "Space Platform Hub with 4x Asteroid Collectors, 2x Crushers, and solar arrays.",
        blueprintString = "0eNqllMtugzAQRX/F8tpEGEhasquy6bJSllVUGXAbS8ZGtmmDIv695tH0kVDazAr5Mcd3ri5zxJmseWWEcnh9xMLxEq+/7BEsWcal39tWLOfoQTL3rE2Jto4Zxw26rzN/qeA2N6JyQqvzq/4KehNuj5IDurO+SIsCbbSUPHfaWIKiA9qY2u55t2CqQFZLZhAzhjV24fGv/qRHL1dRmqTpMk4iGtOIYJFrZfH60UtXBT/gNSXYihfFZNeOayru9fRdEaxY2a1spy6oRnXB3jfQtuQEiGYBbOwhyD968IAdwVw54QQf5PSL5knVZcZNL2v6eYIrbcVg3hF7DeEiJLjpv720H7DoNylnsGA10oLkMi7+F26OllwnboK2vErbBGx1guVD3C7oiUfE6jLiZh4xQ7j9TEKX8qBiyv9f00KCCU76N853jE+pE3KM6OVAPutaFawnnIuKBlbUC7q+nsLqQ1g58PWrm6dA8yjQPAozj8LMoyDzQph3Icy6EORcCDIuBPkGzBwwcrDEwQIHyxtw0AHnHGzMwabcacjt2vYd+VNkZQ=="
    )

    val library: List<SampleBlueprintEntry> = listOf(
        QUAD_NUCLEAR,
        GREEN_CIRCUIT_ARRAY,
        KOVAREX_ENRICHMENT,
        SPACE_PLATFORM_HUB
    )
}
