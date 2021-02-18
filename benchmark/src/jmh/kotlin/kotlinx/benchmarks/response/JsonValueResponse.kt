package response

import kotlinx.serialization.json.JsonElement

interface JsonValueResponse {

    var jsonValue: JsonElement?

}