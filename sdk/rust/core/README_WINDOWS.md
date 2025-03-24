## Secrets Manager -  Rust
This SDK helps you retrieve and manage your secrets from keeper.

### How to get it to work locally
To get it to work locally we need to have
* Rust installed
* cargo installed
* rustc installed

This SDK uses openssl on your Windows machine. We compile openssl directly from source during the build process and for this you need perl to be installed on your windows machine. To get this working you need to install the Perl package from [here](https://strawberryperl.com/releases.html). 

### Examples of code usage
Code usage examples can be found [here](./examples/usage_examples.md)

### Common errors and debugging:
1. `linker not found error
```
error: linker `link.exe` not found                                                                                                                                                                                                                                                               
  |
  = note: program not found

note: the msvc targets depend on the msvc linker but `link.exe` was not found

note: please ensure that Visual Studio 2017 or later, or Build Tools for Visual Studio were installed with the Visual C++ option.
```
If this error is happening, then your visual studio build tools are not found on your windows machine. since we use mvsc as default build chain, you need to install the build tools for visual studio. You can download it from [here](https://visualstudio.microsoft.com/downloads/). and guide can be found [here](https://devblogs.microsoft.com/cppblog/introducing-the-visual-studio-build-tools/).