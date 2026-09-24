class_name PineWorldLayout
extends RefCounted

const ROAD_Y := 0.05
const VEHICLE_REST_Y := 0.60
const MAP_HALF_SIZE := 2250.0

const BOB := Vector3(-1060.0,0.0,-690.0)
const BOB_EVENT := Vector3(-1000.0,0.085,-650.0)
const PROTAGONIST_HOME := Vector3(-1040.0,0.0,1010.0)
const HOME_EVENT := Vector3(-980.0,0.085,950.0)
const JIM := Vector3(1010.0,0.0,1320.0)
const JIM_EVENT := Vector3(950.0,0.085,1250.0)
const BBQ := Vector3(-120.0,0.0,-330.0)
const BBQ_EVENT := Vector3(-35.0,0.085,-330.0)
const TOWN_HALL := Vector3(105.0,0.0,-105.0)
const TOWN_HALL_EVENT := Vector3(20.0,0.085,-105.0)
const TOWN_HALL_2 := Vector3(-420.0,0.0,75.0)
const TOWN_HALL_3 := Vector3(430.0,0.0,80.0)
const PUBLIC_WORKS := Vector3(815.0,0.0,-770.0)
const PUBLIC_WORKS_EVENT := Vector3(760.0,0.085,-720.0)
const GAS_STATION := Vector3(120.0,0.0,205.0)
const GAS_EVENT := Vector3(25.0,0.085,205.0)
const GENERAL_STORE := Vector3(-135.0,0.0,165.0)
const GENERAL_STORE_EVENT := Vector3(-35.0,0.085,165.0)

const TEST_ROUTE_A := Vector3(-1180.0,0.085,-290.0)
const TEST_ROUTE_B := Vector3(-805.0,0.085,0.0)
const DEEP_SNOW_SITE := Vector3(1590.0,0.085,1530.0)
const FOREST_SEARCH_A := Vector3(1450.0,0.085,1540.0)
const FOREST_SEARCH_B := Vector3(1780.0,0.085,1440.0)

static func road_segments() -> Array:
    return [
        {"name":"Main Street","a":Vector3(0,0,-1250),"b":Vector3(0,0,1250),"width":10.0},
        {"name":"Town Crossroad","a":Vector3(-800,0,0),"b":Vector3(950,0,0),"width":10.0},
        {"name":"West Highway A","a":Vector3(-800,0,0),"b":Vector3(-1250,0,-350),"width":10.0},
        {"name":"West Highway B","a":Vector3(-1250,0,-350),"b":Vector3(-1750,0,-800),"width":10.0},
        {"name":"Bob Spur","a":Vector3(-1250,0,-350),"b":Vector3(-1000,0,-650),"width":9.0},
        {"name":"Home Road A","a":Vector3(-800,0,0),"b":Vector3(-1250,0,500),"width":9.0},
        {"name":"Home Road B","a":Vector3(-1250,0,500),"b":Vector3(-980,0,950),"width":9.0},
        {"name":"Jim Road A","a":Vector3(0,0,850),"b":Vector3(650,0,900),"width":9.0},
        {"name":"Jim Road B","a":Vector3(650,0,900),"b":Vector3(950,0,1250),"width":9.0},
        {"name":"Forest Road A","a":Vector3(950,0,1250),"b":Vector3(1500,0,1600),"width":8.0},
        {"name":"Forest Road B","a":Vector3(1500,0,1600),"b":Vector3(2000,0,1250),"width":8.0},
        {"name":"East Snow Road A","a":Vector3(950,0,0),"b":Vector3(1400,0,400),"width":9.0},
        {"name":"East Snow Road B","a":Vector3(1400,0,400),"b":Vector3(1750,0,900),"width":8.5},
        {"name":"Public Works Road A","a":Vector3(0,0,-650),"b":Vector3(650,0,-650),"width":9.0},
        {"name":"Public Works Road B","a":Vector3(650,0,-650),"b":Vector3(900,0,-900),"width":9.0},
        {"name":"North Residential 1","a":Vector3(-300,0,300),"b":Vector3(300,0,300),"width":8.0},
        {"name":"North Residential 2","a":Vector3(300,0,300),"b":Vector3(300,0,650),"width":8.0},
        {"name":"North Residential 3","a":Vector3(300,0,650),"b":Vector3(-300,0,650),"width":8.0},
        {"name":"North Residential 4","a":Vector3(-300,0,650),"b":Vector3(-300,0,300),"width":8.0},
        {"name":"South Residential 1","a":Vector3(-300,0,-250),"b":Vector3(350,0,-250),"width":8.0},
        {"name":"South Residential 2","a":Vector3(350,0,-250),"b":Vector3(350,0,-500),"width":8.0},
        {"name":"South Residential 3","a":Vector3(350,0,-500),"b":Vector3(-300,0,-500),"width":8.0},
        {"name":"South Residential 4","a":Vector3(-300,0,-500),"b":Vector3(-300,0,-250),"width":8.0}
    ]

static func start_transform() -> Transform3D:
    return nearest_road_transform(BOB_EVENT)

static func nearest_road_transform(from_pos: Vector3) -> Transform3D:
    var best_distance := INF
    var best_point := Vector3.ZERO
    var best_yaw := 0.0
    for segment in road_segments():
        var a: Vector3 = segment["a"]
        var b: Vector3 = segment["b"]
        var ab := Vector2(b.x-a.x,b.z-a.z)
        var p := Vector2(from_pos.x-a.x,from_pos.z-a.z)
        var denom := ab.length_squared()
        var t := 0.0 if denom <= 0.0001 else clampf(p.dot(ab)/denom,0.0,1.0)
        var point2 := Vector2(a.x,a.z) + ab*t
        var distance := Vector2(from_pos.x-point2.x,from_pos.z-point2.y).length()
        if distance < best_distance:
            best_distance = distance
            best_point = Vector3(point2.x,VEHICLE_REST_Y,point2.y)
            var direction := (b-a).normalized()
            best_yaw = atan2(direction.x,direction.z)
    return Transform3D(Basis(Vector3.UP,best_yaw),best_point)

static func total_road_length_m() -> float:
    var total := 0.0
    for segment in road_segments():
        var a: Vector3 = segment["a"]
        var b: Vector3 = segment["b"]
        total += a.distance_to(b)
    return total
