CREATE DATABASE plates_detection;

\c plates_detection;

-- Tabla de Propietarios
CREATE TABLE owners (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabla de Placas
CREATE TABLE plates (
    id UUID PRIMARY KEY,
    plate_number VARCHAR(20) UNIQUE NOT NULL,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX plates_plate_number_index ON plates(plate_number);
CREATE INDEX plates_owner_id_index ON plates(owner_id);