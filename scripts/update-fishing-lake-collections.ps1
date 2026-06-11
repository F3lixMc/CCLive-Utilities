$path = Join-Path $PSScriptRoot "..\src\main\resources\assets\cclive-utilities\items.json"

$koeder = "K$([char]0x00F6)der"

function Get-TypeRoman([string]$type) {
    switch ($type) {
        'Rute'    { return 'I' }
        'Schnur'  { return 'II' }
        'Leine'   { return 'II' }
        'Rolle'   { return 'III' }
        'Haken'   { return 'IV' }
        'Pose'    { return 'V' }
        'Spinner' { return 'VI' }
        'Gewicht' { return 'VII' }
        default {
            if ($type -eq $koeder) { return 'VIII' }
            return $null
        }
    }
}

function Get-PrismarinCollection([string]$type) {
    switch ($type) {
        'Rute'    { return 'Blutwasser See IX' }
        'Schnur'  { return 'Sporenweiher IX' }
        'Leine'   { return 'Sporenweiher IX' }
        'Rolle'   { return 'Nachtschatten See IX' }
        'Haken'   { return 'Goldteich IX' }
        'Pose'    { return 'Wurzelbucht IX' }
        'Spinner' { return 'Sulfurdampf See IX' }
        'Gewicht' { return 'Infernoweiher IX' }
        default {
            if ($type -eq $koeder) { return 'Lava-Quellsee IX' }
            return $null
        }
    }
}

$aschensuempfe = "Aschens$([char]0x00FC)mpfe"
$russwasserteich = "Ru$([char]0x00DF)wasserteich"

$lakeGroups = @(
    @{ prefixes = @('Aschengruben', 'Glutgruben'); location = 'Infernoweiher' }
    @{ prefixes = @($aschensuempfe, 'Aschteich'); location = $russwasserteich }
    @{ prefixes = @('Blutgezeiten', 'Blutflut'); location = 'Blutwasser See' }
    @{ prefixes = @('Dampfventil'); location = 'Sulfurdampf See' }
    @{ prefixes = @('Auric', 'Gold'); location = 'Goldteich' }
    @{ prefixes = @('Magma'); location = 'Lava-Quellsee' }
    @{ prefixes = @('Nebelwasser'); location = 'Nebelsee' }
    @{ prefixes = @('Perlen', 'Perlmutt'); location = 'Perlenbucht' }
    @{ prefixes = @('Schatten'); location = 'Nachtschatten See' }
    @{ prefixes = @('Schimmertiefen'); location = 'Erzschimmer See' }
    @{ prefixes = @('Seebett', 'Seegrund'); location = 'Smaragdsee' }
    @{ prefixes = @('Sporenwasser'); location = 'Sporenweiher' }
    @{ prefixes = @('Wirrwurzel'); location = 'Wurzelbucht' }
)

function Get-CollectionForItem([string]$itemName) {
    if ([string]::IsNullOrWhiteSpace($itemName)) { return $null }
    if ($itemName -eq "Auric-$koeder") { return '???' }

    $dash = $itemName.IndexOf('-')
    if ($dash -lt 0) { return $null }
    $prefix = $itemName.Substring(0, $dash)
    $type = $itemName.Substring($dash + 1)

    if ($prefix -eq 'Prismarin') {
        return Get-PrismarinCollection $type
    }

    foreach ($group in $lakeGroups) {
        if ($group.prefixes -contains $prefix) {
            $roman = Get-TypeRoman $type
            if ($roman) {
                return "$($group.location) $roman"
            }
        }
    }

    return $null
}

$nameToCollection = @{}
$json = Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($item in $json.fishing_components) {
    $collection = Get-CollectionForItem $item.name
    if ($null -ne $collection) {
        $nameToCollection[$item.name] = $collection
    }
}

$lines = [System.Collections.Generic.List[string]](Get-Content $path -Encoding UTF8)
$inFishing = $false
$pendingCollection = $null
$updated = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match '^\s*"fishing_components":\s*\[') { $inFishing = $true }
    elseif ($inFishing -and $line -match '^\s*"fish_traps":') { $inFishing = $false; $pendingCollection = $null }
    if ($inFishing -and $line -match '^\s*"name":\s*"(.+)",\s*$') {
        $itemName = $matches[1]
        if ($nameToCollection.ContainsKey($itemName)) {
            $pendingCollection = $nameToCollection[$itemName]
        } else {
            $pendingCollection = $null
        }
    }
    if ($null -ne $pendingCollection -and ($line -match '^\s*"floor":\s*""' -or $line -match '^\s*"collection":\s*"')) {
        $indent = ($line -replace '(\s*).*', '$1')
        $lines[$i] = "${indent}`"collection`": `"$pendingCollection`""
        $updated++
        $pendingCollection = $null
    }
}

[System.IO.File]::WriteAllLines($path, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Output "Updated $updated fishing component lake collections"
$nameToCollection.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name) -> $($_.Value)" }
