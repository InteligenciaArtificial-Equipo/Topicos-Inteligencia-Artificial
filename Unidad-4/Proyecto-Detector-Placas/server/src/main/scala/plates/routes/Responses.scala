package plates.routes

import plates.models.PlateWithOwner

case class ApiResponse(success: Boolean, message: String, data: Option[PlateWithOwner] = None)
