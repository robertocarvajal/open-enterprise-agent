{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    std.url = "github:divnix/std";
  };

  outputs = inputs:
    inputs.std.growOn {
      inherit inputs;
      cellsFrom = ./nix;
      cellBlocks = [
        (inputs.std.devshells "devshell")
        (inputs.std.installables "packages")
      ];
    }
    {
      devShells = inputs.std.harvest inputs.self ["automation" "devshell"];
    };
}
