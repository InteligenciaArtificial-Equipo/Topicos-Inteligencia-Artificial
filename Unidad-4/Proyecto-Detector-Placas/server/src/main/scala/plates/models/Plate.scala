package plates.models

import java.util.UUID

case class Plate(id: UUID, plateNumber: String, ownerId: UUID)
