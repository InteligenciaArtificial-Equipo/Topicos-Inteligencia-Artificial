package plates.routes

case class RegisterRequest(plateNumber: String, ownerName: String)
case class QueryRequest(imageBase64: String)
case class QueryByPlateRequest(plateNumber: String)