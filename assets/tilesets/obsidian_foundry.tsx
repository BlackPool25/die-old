<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.11.0" name="obsidian_foundry" tilewidth="64" tileheight="96" tilecount="10" columns="4">
  <image source="obsidian_foundry.png" width="256" height="256"/>
  
  <!-- Tile 0: Basalt Flagstone 01 -->
  <tile id="0">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="basalt"/>
    </properties>
  </tile>

  <!-- Tile 1: Basalt Flagstone 02 -->
  <tile id="1">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="basalt"/>
    </properties>
  </tile>

  <!-- Tile 2: Obsidian Border -->
  <tile id="2">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="obsidian_iron"/>
    </properties>
  </tile>

  <!-- Tile 3: Molten Heat Vent (Hazard) -->
  <tile id="3">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="hazard" type="bool" value="true"/>
      <property name="hazard_type" value="molten_slag"/>
      <property name="damage" type="int" value="15"/>
      <property name="tick_interval" type="float" value="0.5"/>
    </properties>
  </tile>

  <!-- Tile 4: Rune Circle -->
  <tile id="4">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="chrono_rune" type="bool" value="true"/>
    </properties>
  </tile>

  <!-- Tile 5: Cast Iron Wall NW (Box2D Collider) -->
  <tile id="5">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <!-- Box2D Diamond Footprint Collider -->
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 6: Cast Iron Wall NE (Box2D Collider) -->
  <tile id="6">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 7: Wall Corner Outer (Box2D Collider) -->
  <tile id="7">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 8: Wall Furnace Vent (Box2D Collider & Heat Source) -->
  <tile id="8">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="light_emitter" type="bool" value="true"/>
      <property name="light_color" value="#FF6600"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 9: Basalt Pillar (Box2D Circular/Octagonal Base Collider) -->
  <tile id="9">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="pillar" type="bool" value="true"/>
      <property name="y_sort_offset" type="int" value="16"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <!-- 2.5D Base Collider at Pillar Feet -->
      <object id="1" x="8" y="72">
        <polygon points="12,0 36,0 48,12 36,24 12,24 0,12"/>
      </object>
    </objectgroup>
  </tile>
</tileset>
