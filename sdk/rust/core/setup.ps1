# Requires -RunAsAdministrator

Write-Host "== Windows Rust Build Environment Setup ==" -ForegroundColor Cyan

# --- Check & Install Perl 5.38 ---
$perlVersionRequired = "5.38"
$perlPath = "C:\Strawberry\perl\bin\perl.exe"

if (Test-Path $perlPath) {
    try {
        $installedVersion = & $perlPath -e "print substr(`$^V, 1)" # Remove 'v' prefix from version string
        if ($installedVersion -like "$perlVersionRequired*") {
            Write-Host "[OK] Perl $installedVersion found." -ForegroundColor Green
        } else {
            Write-Host "[WARNING] Different Perl version found: $installedVersion" -ForegroundColor Yellow
            Write-Host "[INFO] Installing Strawberry Perl $perlVersionRequired..." -ForegroundColor Cyan
            Invoke-WebRequest -Uri "https://github.com/StrawberryPerl/Perl-Dist-Strawberry/releases/download/SP_53822_64bit/strawberry-perl-5.38.2.2-64bit.msi" -OutFile "$env:TEMP\perl-installer.msi"
            Start-Process msiexec.exe -Wait -ArgumentList "/i `"$env:TEMP\perl-installer.msi`" /qn"
            Remove-Item "$env:TEMP\perl-installer.msi"
            Write-Host "[OK] Perl installed." -ForegroundColor Green
        }
    } catch {
        Write-Host "[ERROR] Failed to check Perl version: $_" -ForegroundColor Red
        Write-Host "[INFO] Installing Strawberry Perl $perlVersionRequired..." -ForegroundColor Cyan
        Invoke-WebRequest -Uri "https://github.com/StrawberryPerl/Perl-Dist-Strawberry/releases/download/SP_53822_64bit/strawberry-perl-5.38.2.2-64bit.msi" -OutFile "$env:TEMP\perl-installer.msi"
        Start-Process msiexec.exe -Wait -ArgumentList "/i `"$env:TEMP\perl-installer.msi`" /qn"
        Remove-Item "$env:TEMP\perl-installer.msi"
        Write-Host "[OK] Perl installed." -ForegroundColor Green
    }
} else {
    Write-Host "[INFO] Installing Strawberry Perl $perlVersionRequired..." -ForegroundColor Cyan
    try {
        Invoke-WebRequest -Uri "https://github.com/StrawberryPerl/Perl-Dist-Strawberry/releases/download/SP_53822_64bit/strawberry-perl-5.38.2.2-64bit.msi" -OutFile "$env:TEMP\perl-installer.msi"
        Start-Process msiexec.exe -Wait -ArgumentList "/i `"$env:TEMP\perl-installer.msi`" /qn"
        Remove-Item "$env:TEMP\perl-installer.msi"
        Write-Host "[OK] Perl installed." -ForegroundColor Green
        
        # Refresh PATH to include the newly installed Perl
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    } catch {
        Write-Host "[ERROR] Failed to install Perl: $_" -ForegroundColor Red
        exit 1
    }
}

# --- Check & Install MSVC Build Tools ---
$vsInstallerPath = "$env:TEMP\vs_buildtools.exe"
$vsInstallArgs = "--quiet --wait --norestart --nocache --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --installPath C:\BuildTools"

# Better check for MSVC Build Tools - check for cl.exe in multiple possible locations
$clPaths = @(
    "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe",
    "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe",
    "${env:ProgramFiles}\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe",
    "${env:ProgramFiles}\Microsoft Visual Studio\2019\BuildTools\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe",
    "C:\BuildTools\VC\Tools\MSVC\*\bin\Hostx64\x64\cl.exe"
)

$clFound = $false
foreach ($path in $clPaths) {
    if (Get-Item -Path $path -ErrorAction SilentlyContinue) {
        $clFound = $true
        break
    }
}

if ($clFound -or (Get-Command cl.exe -ErrorAction SilentlyContinue)) {
    Write-Host "[OK] MSVC compiler detected (cl.exe)." -ForegroundColor Green
} else {
    Write-Host "[INFO] Installing MSVC Build Tools..." -ForegroundColor Cyan
    Write-Host "[INFO] this takes a while......." -ForegroundColor Cyan
    try {
        Invoke-WebRequest -Uri "https://aka.ms/vs/17/release/vs_buildtools.exe" -OutFile $vsInstallerPath
        Start-Process -FilePath $vsInstallerPath -ArgumentList $vsInstallArgs -Wait -NoNewWindow
        Remove-Item $vsInstallerPath
        
        # Check again after installation attempt
        $clFound = $false
        foreach ($path in $clPaths) {
            if (Get-Item -Path $path -ErrorAction SilentlyContinue) {
                $clFound = $true
                break
            }
        }
        
        if ($clFound) {
            Write-Host "[OK] Build Tools installed." -ForegroundColor Green
            
            # Add to PATH if not already there
            $msvcPath = (Get-Item -Path ($clPaths | Where-Object { Test-Path $_ -ErrorAction SilentlyContinue } | Select-Object -First 1)).Directory.FullName
            if ($msvcPath -and (-not $env:Path.Contains($msvcPath))) {
                $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
                [System.Environment]::SetEnvironmentVariable("Path", "$userPath;$msvcPath", "User")
                $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
            }
        } else {
            Write-Host "[WARNING] MSVC Build Tools installation may have failed. Please install manually." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[ERROR] Failed to install MSVC Build Tools: $_" -ForegroundColor Red
    }
}

# --- Check & Install Rustup ---
if (-not (Get-Command rustc.exe -ErrorAction SilentlyContinue)) {
    Write-Host "[INFO] Installing Rust..." -ForegroundColor Cyan
    try {
        $rustupInit = "$env:TEMP\rustup-init.exe"
        Invoke-WebRequest https://win.rustup.rs -OutFile $rustupInit
        Start-Process $rustupInit -ArgumentList "-y", "--default-toolchain", "stable", "--no-modify-path" -Wait
        Remove-Item $rustupInit
        
        # Add to PATH if not already there
        $rustupPath = "$env:USERPROFILE\.cargo\bin"
        if (-not $env:Path.Contains($rustupPath)) {
            $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
            [System.Environment]::SetEnvironmentVariable("Path", "$userPath;$rustupPath", "User")
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
        }
        
        Write-Host "[OK] Rust installed." -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Failed to install Rust: $_" -ForegroundColor Red
    }
} else {
    try {
        $version = & rustc --version
        Write-Host "[OK] Rust already installed: $version" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] rustc.exe is in PATH but may not be working correctly." -ForegroundColor Yellow
    }
}

Write-Host "`n== Setup Complete. You may need to restart your terminal for PATH changes to take effect. ==" -ForegroundColor Cyan