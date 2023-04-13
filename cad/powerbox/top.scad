// OpenSCAD for a "porch" extension for a Turtlebot3 burger
//           It includes a lampholder.
thickness = 4;
lampholder_thickness = 3; // Must be smaller than distance between nubs
bulb_width = 25;
porch_center_y = 36;  // The length of the rectangular portion
porch_x  = 96;
rivet_radius = 2.5;   // Hole radius for rivet attachment
round_radius = 2;     // Radius of the corner arcs
tab_x = 78;           // center-center
tab_y = 7;            // edge to center of holes
tabr_x = 22.25;       // center-to-center of relay board
tabr_y = -19;         // center to center of relay board
rim = 3;              // Width of rim
hex_offset = 12;      // Center-to-center of hex connection
hex_radius = 2.5;     // Radius for hex nuts
post_offset = 24;     // Center-to-center of post cutout

// This is the basic plate. Y argument is the protrusion (2/3 of the
// porch extent). This object cannot be translated once created.
module base(x,y,z) {
    $fn=100;
    linear_extrude(height=z,center=false,convexity=10) {
        hull() {
        // place 6 circles in the corners, with the rounding radius
        translate([(-x/2)+(round_radius/2), -y+(round_radius/2), 0])
        circle(r=round_radius);
        
        translate([(x/2)-(round_radius/2), -y+(round_radius/2), 0])
        circle(r=round_radius);

        translate([(x/2)-(round_radius/2), 0-(round_radius/2), 0])
        circle(r=round_radius);
        
        translate([x/2-y+(round_radius/2), y/2-(round_radius/2), 0])
        circle(r=round_radius);

        translate([(-x/2)+y-(round_radius/2),y/2-(round_radius/2), 0])
        circle(r=round_radius);
     
        translate([(-x/2)+(round_radius/2), 0+(round_radius/2), 0])
        circle(r=round_radius);
        }
    }
}

// This is the basic plate less a rim width all around. This is meant to
// be subtracted from the standard plate.
module base_cutout(x,y,z,rwidth) {
    $fn=100;
    linear_extrude(height=z,center=false,convexity=10) {
        hull() {
        // place 6 circles in the corners, with the rounding radius
        translate([(-x/2)+(round_radius/2)+rwidth, -y+(round_radius/2)+rwidth, 0])
        circle(r=round_radius);
        
        translate([(x/2)-(round_radius/2)-rwidth, -y+(round_radius/2)+rwidth, 0])
        circle(r=round_radius);

        translate([(x/2)-(round_radius/2)-rwidth, 0-(round_radius/2)-rwidth/2, 0])
        circle(r=round_radius);
        
        translate([x/2-y+(round_radius/2), y/2-(round_radius/2)-rwidth, 0])
        circle(r=round_radius);

        translate([(-x/2)+y-(round_radius/2),y/2-(round_radius/2)-rwidth, 0])
        circle(r=round_radius);
     
        translate([(-x/2)+(round_radius/2)+rwidth, 0+(round_radius/2)-rwidth/2, 0])
        circle(r=round_radius);
        }
    }
}

// x - center to between holes
// y - baseline to center of hole
// z - thickness
module holes(x,y,z) {
    pad = 0.5;
    // These are rivet holes for the tab pieces
    for(i=[[x+rivet_radius+pad,y],[x-rivet_radius-pad,y],
           [-x+rivet_radius+pad,y],[-x-rivet_radius-pad,y]]) {
        translate(i)
        cylinder(r=rivet_radius,h=z);
    }
}

// x - center of board
// y - center of board
// z - thickness
module relay_holes(x,y,z) {
    dx = 14;   // center of board to center of hole
    dy = 10;   // center of board to center of hole
    // These are rivet holes for the relay board
    for(i=[[x+dx,y+dy],[x-dx,y-dy],
           [x+dx,y-dy],[x-dx,y+dy]]) {
        translate(i)
        cylinder(r=rivet_radius,h=z);
    }
}


// Protrusions to allow screws to main platform
// Add filler an extra 2mm to sides and back
module connector(z) {
    translate([0,4,0])
        cube([12,5,thickness],center=true);
    translate([-4,2,0])
        cube([4,4,thickness],center=true);
    translate([4,2,0])
        cube([4,4,thickness],center=true);
    difference() {
        rotate(22.5,0,0)
        cylinder(h=z,r1=5,r2=5,$fn=8,center=true);
        cylinder(h=z,r1=2,r2=2,center=true);
    }
}
// Subtract this from the base platform
// These are the cutouts for the fastening nuts
module connector_housing(z) {
    linear_extrude(height=z,center=true) {
        rotate(22.5,0,0)
        circle(r=6,$fn=8);
    }
}
// Subtract this from the base platform
// This is the cutout for posts.
module post_housing(z) {
    linear_extrude(height=z,center=true) {
        rotate(22.5,0,0)
        circle(r=6,$fn=8);
    }
}
// width - radius of the bulb opening
// z - thickness of the holder
module lampholder(width,z) {
    bulb_tail_radius = 8;
    notch_radius = 1;
    difference() {
        union() {
            translate([0,-width/4,0])
                cube([width,width/2,z],center=true);
            translate([0,-width/2,-z/2])
                cylinder(r=width/2,h=z);
           }
           translate([0,-width/2,-z/2])
            cylinder(r=bulb_tail_radius,h=z);
           translate([0,-width/2-bulb_tail_radius-notch_radius/2,-z/2])
            cylinder(r=notch_radius,h=z);
    }
}
// Cutout to account for flare of lamp. 15mm->28mm in 5mm
module lamp_flare() {
    cylinder(h=6,r1=14,r2=7.5,center=true);
}

// Support between the lampholders.
module support(x,y,z) {
    height=6;
    cube([x,y,height],center=true);
}
  
// --------------------- Final Assembly ----------------------
module final_assembly() {
// Baseplate
translate([0,0,thickness/2])
difference() { 
    base(porch_x,porch_center_y,thickness);
    translate([0,-porch_center_y,0])
        holes(tab_x/2,tab_y,thickness);
        relay_holes(tabr_x,tabr_y,thickness);
    translate([-hex_offset,-porch_center_y-(hex_radius/2),thickness/2])
        connector_housing(2*thickness);
    translate([hex_offset,-porch_center_y-(hex_radius/2),thickness/2])
        connector_housing(2*thickness);
    translate([post_offset,-porch_center_y-(round_radius/2),thickness/2])
        post_housing(2*thickness);
     translate([-post_offset,-porch_center_y-(round_radius/2),thickness])
         post_housing(2*thickness);
}
// Rim
translate([0,0,-thickness/2]) {
    difference() {
        base(porch_x,porch_center_y,thickness);
        base_cutout(porch_x,porch_center_y,thickness,rim);
        translate([hex_offset,-porch_center_y-(hex_radius/2),thickness/2])
            connector_housing(2*thickness);
        translate([-hex_offset,-porch_center_y-(hex_radius/2),thickness/2])
            connector_housing(2*thickness);
        translate([post_offset,-porch_center_y-(round_radius/2),thickness/2])
            post_housing(2*thickness);
        translate([-post_offset,-porch_center_y-(round_radius/2),thickness])
            post_housing(2*thickness);
    }
}
// Lampholders
// Inner distance between holders = 6mm
translate([0,0,thickness/2])
    rotate(90,[1,0,0]) 
        lampholder(bulb_width,lampholder_thickness);
translate([0,6+lampholder_thickness+thickness/2,thickness/2])
    rotate(90,[1,0,0]) 
      lampholder(bulb_width,thickness);


    
translate([bulb_width/2-rim/2,porch_center_y/4,0])
    support(rim,porch_center_y/2,6);
translate([-bulb_width/2+rim/2,porch_center_y/4,0])
    support(rim,porch_center_y/2,6);
// These are the two protrusions for fastening to the robot.
translate([hex_offset,-porch_center_y-hex_radius/2,thickness]) 
    rotate(180,[0,1,0])
        connector(thickness);
translate([-hex_offset,-porch_center_y-hex_radius/2,0])
    connector(thickness);
}

difference() {
    final_assembly();
// Subtract this  
 translate([0,6+2*lampholder_thickness+thickness,-bulb_width/2+3])
  rotate(90,[1,0,0]) 
    lamp_flare();
}