use std::env;
use std::path::PathBuf;
use std::process::{Command};

fn main() {
    println!("executing prebuild checks....");

    if cfg!(target_os = "windows") {
        setup_and_check_msvc_tools();
        check_perl();
    } else if cfg!(target_family = "unix") {
        check_perl();
    }
}

fn check_perl() {
    println!("Checking for Perl...");
    match Command::new("perl").arg("-v").output() {
        Ok(output) => {
            if output.status.success() {
                let version_str = String::from_utf8_lossy(&output.stdout);
                
                // More flexible version checking - look for 5.38 anywhere in the version string
                if version_str.contains("5.38") {
                    println!("[OK] Found Perl version that includes 5.38");
                } else {
                    println!("Perl version found, but may not be 5.38:");
                    println!("{}", version_str);
                    panic!("Perl 5.38 is required. Please install the correct version.");
                }
            } else {
                let error = String::from_utf8_lossy(&output.stderr);
                panic!("Perl check failed: {}", error);
            }
        }
        Err(e) => {
            panic!("Failed to execute Perl: {}. Make sure Perl 5.38 is installed and in your PATH.", e);
        }
    }
}

fn setup_and_check_msvc_tools() {
    println!("Setting up and checking MSVC environment...");
    
    // Check if we're already in a Visual Studio environment
    if env::var("VCINSTALLDIR").is_ok() {
        println!("Visual Studio environment already set up");
    } else {
        // Try to locate and run vcvarsall.bat to set up the environment
        setup_msvc_environment().unwrap_or_else(|e| {
            panic!("Failed to set up MSVC environment: {}", e);
        });
    }
    
    // Now check for the tools with the environment properly set up
    println!("Checking for cl.exe...");
    check_cl_exists(); // Simplified check
    println!("Checking for cl.exe...");
    check_link_exists(); // Simplified check
    check_nmake_exists(); // Simplified check
}

fn setup_msvc_environment() -> Result<(), String> {
    
    // Common paths where vcvarsall.bat might be located
    let vs_paths = [
        r"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build",
        r"C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build",
        r"C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build",
        r"C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2019\Enterprise\VC\Auxiliary\Build",
        r"C:\Program Files (x86)\Microsoft Visual Studio\2019\Professional\VC\Auxiliary\Build",
    ];
    
    // Try to find vcvarsall.bat
    for vs_path in &vs_paths {
        let vcvarsall = PathBuf::from(vs_path).join("vcvarsall.bat");
        if vcvarsall.exists() {
            println!("Found vcvarsall.bat at: {}", vcvarsall.display());
            
            // Create a batch file that runs vcvarsall and then sets environment variables for this process
            let temp_dir = env::temp_dir();
            let batch_path = temp_dir.join("vs_env_setup.bat");
            let output_path = temp_dir.join("vs_env_vars.txt");
            
            // Architecture - use x64 for 64-bit builds
            let arch = "x64";
            
            // Create batch file content
            let batch_content = format!(
                "@echo off\r\n\
                call \"{}\" {}\r\n\
                set > \"{}\"\r\n",
                vcvarsall.display(), arch, output_path.display()
            );
            
            // Write batch file
            std::fs::write(&batch_path, batch_content)
                .map_err(|e| format!("Failed to write setup batch file: {}", e))?;
            
            // Execute batch file
            let status = Command::new("cmd")
                .args(&["/c", batch_path.to_str().unwrap()])
                .status()
                .map_err(|e| format!("Failed to execute setup batch file: {}", e))?;
            
            if !status.success() {
                return Err(format!("vcvarsall.bat execution failed with status: {}", status));
            }
            
            // Read environment variables from output file
            let env_vars = std::fs::read_to_string(&output_path)
                .map_err(|e| format!("Failed to read environment variables: {}", e))?;
            
            // Parse and set environment variables
            for line in env_vars.lines() {
                if let Some(pos) = line.find('=') {
                    let key = &line[0..pos];
                    let value = &line[pos+1..];
                    env::set_var(key, value);
                }
            }
            
            // Clean up
            let _ = std::fs::remove_file(batch_path);
            let _ = std::fs::remove_file(output_path);
            
            return Ok(());
        }
    }
    
    Err("Could not find vcvarsall.bat in any of the expected locations".to_string())
}

// Simplified: Just check if cl.exe exists and runs
fn check_cl_exists() {
    println!("Checking for cl.exe...");
    let cl_result = Command::new("where")
        .arg("cl.exe")
        .output();
    
    match cl_result {
        Ok(output) => {
            if output.status.success() {
                let path = String::from_utf8_lossy(&output.stdout);
                println!("[OK] Found cl.exe at: {}", path.trim());
            } else {
                // Try a simpler check - attempt to run cl.exe
                match Command::new("cl").arg("/?").output() {
                    Ok(_) => {
                        println!("[OK] cl.exe is available");
                    }
                    Err(e) => {
                        panic!("MSVC cl.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                    }
                }
            }
        }
        Err(_) => {
            // On some Windows versions, 'where' might not work as expected
            // Try a simpler check - attempt to run cl.exe
            match Command::new("cl").arg("/?").output() {
                Ok(_) => {
                    println!("[OK] cl.exe is available");
                }
                Err(e) => {
                    panic!("MSVC cl.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                }
            }
        }
    }
}

// Simplified: Just check if link.exe exists and runs
fn check_link_exists() {
    println!("Checking for link.exe...");
    let link_result = Command::new("where")
        .arg("link.exe")
        .output();
    
    match link_result {
        Ok(output) => {
            if output.status.success() {
                let path = String::from_utf8_lossy(&output.stdout);
                println!("[OK] Found link.exe at: {}", path.trim());
            } else {
                // Try a simpler check
                match Command::new("link").output() {
                    Ok(_) => {
                        println!("[OK] link.exe is available");
                    }
                    Err(e) => {
                        panic!("MSVC link.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                    }
                }
            }
        }
        Err(_) => {
            // Try a simpler check
            match Command::new("link").output() {
                Ok(_) => {
                    println!("[OK] link.exe is available");
                }
                Err(e) => {
                    panic!("MSVC link.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                }
            }
        }
    }
}

// Simplified: Just check if nmake.exe exists and runs
fn check_nmake_exists() {
    println!("Checking for nmake.exe...");
    let nmake_result = Command::new("where")
        .arg("nmake.exe")
        .output();
    
    match nmake_result {
        Ok(output) => {
            if output.status.success() {
                let path = String::from_utf8_lossy(&output.stdout);
                println!("[OK] Found nmake.exe at: {}", path.trim());
            } else {
                // Try a simpler check
                match Command::new("nmake").arg("/?").output() {
                    Ok(_) => {
                        println!("[OK] nmake.exe is available");
                    }
                    Err(e) => {
                        panic!("MSVC nmake.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                    }
                }
            }
        }
        Err(_) => {
            // Try a simpler check
            match Command::new("nmake").arg("/?").output() {
                Ok(_) => {
                    println!("[OK] nmake.exe is available");
                }
                Err(e) => {
                    panic!("MSVC nmake.exe not found: {}. Make sure you have Visual Studio with C++ development tools installed.", e);
                }
            }
        }
    }
}