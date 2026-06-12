$root = Join-Path $PSScriptRoot "..\src\main\resources\assets\cclive-utilities"
$kitsPath = Join-Path $root "Kits.json"
$bpPath = Join-Path $root "blueprints.json"
$outPath = Join-Path $root "neu_kits_fragment.json"

$kits = Get-Content $kitsPath -Raw -Encoding UTF8 | ConvertFrom-Json
$bp = Get-Content $bpPath -Raw -Encoding UTF8 | ConvertFrom-Json

$floorMap = @{}
foreach ($prop in $bp.floors.PSObject.Properties) {
    $m = [regex]::Match($prop.Name, 'floor_(\d+)')
    if (-not $m.Success) { continue }
    $floor = [int]$m.Groups[1].Value
    foreach ($rarity in $prop.Value.blueprints.PSObject.Properties) {
        foreach ($item in $rarity.Value.items) {
            if (-not $floorMap.ContainsKey($item)) { $floorMap[$item] = $floor }
        }
    }
}
$floorMap['Kometenband'] = $floorMap['Kristallring Kometenband']

$itemTypeMap = @{}
foreach ($kitKey in $kits.PSObject.Properties.Name) {
    if ($kitKey -eq 'neu') { continue }
    $kitData = $kits.$kitKey
    if ($null -eq $kitData) { continue }
    foreach ($levelKey in $kitData.PSObject.Properties.Name) {
        $arr = $kitData.$levelKey
        $name = $null
        foreach ($el in $arr) {
            if ($el -is [string]) { $name = $el }
            elseif ($el -is [pscustomobject]) {
                if ($name -and $el.item_type) { $itemTypeMap[$name] = $el.item_type }
                $name = $null
            }
        }
    }
}

function Get-Floor($name) {
    if ($floorMap.ContainsKey($name)) { return "e$($floorMap[$name])" }
    return $null
}
function Get-Rarity($de) {
    switch ($de) {
        'Episch' { 'Epic' }
        'Legendär' { 'Legendary' }
        'Selten' { 'Rare' }
        'Ungewöhnlich' { 'Uncommon' }
        default { $de }
    }
}
function Get-Mods($parts) {
    $p = $parts | Where-Object { $_ -and $_.Trim() }
    if (-not $p -or $p.Count -eq 0) { return '' }
    return (($p | ForEach-Object { "[$_]" }) -join ', ')
}
function New-ItemEntries($items) {
    $result = @()
    foreach ($it in $items) {
        $name, $rarityDe, $mods = $it
        $obj = [ordered]@{ rarity = (Get-Rarity $rarityDe); modifier = (Get-Mods $mods) }
        $f = Get-Floor $name
        if ($f) { $obj.floor = $f }
        if ($itemTypeMap.ContainsKey($name)) { $obj.item_type = $itemTypeMap[$name] }
        $result += $name
        $result += [pscustomobject]$obj
    }
    return $result
}

$neu = [ordered]@{}
$neu['münz_kit'] = [ordered]@{
    '1' = New-ItemEntries @(
        @('Rubinrote Kriegshose','Episch',@('Andere','Andere','Schaden')),
        @('Ewiges Lichtband','Episch',@('Andere','Andere','Schaden')),
        @('Göttliche Plattenläufer','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Rubinrote Kriegsschultern','Episch',@('Andere','Andere','Verteidigung')),
        @('Ewiges Lichtamulett','Episch',@('Andere','Andere','Attribute')),
        @('Rubinrote Kriegshandschuhe','Episch',@('Andere','Andere','Fähigkeiten')),
        @('Blutige Fleischhackeraxt','Selten',@('Andere','Andere')),
        @('Rubinstaub Schrammer','Selten',@('Andere','Verteidigung')),
        @('Rubinroter Kriegshelm','Episch',@('Andere','Andere','Attribute')),
        @('Rubinroter Kriegsharnisch','Episch',@('Andere','Andere','Verteidigung')),
        @('Rubinroter Kriegsgürtel','Episch',@('Andere','Andere','Fähigkeiten')),
        @('Kaktuszermalmer','Selten',@('Andere','Andere'))
    )
    '2' = New-ItemEntries @(
        @('Ewiges Lichtband','Episch',@('Andere','Andere','Schaden')),
        @('Rubinrote Kriegshose','Episch',@('Andere','Andere','Schaden')),
        @('Blutige Fleischhackeraxt','Selten',@('Andere','Andere')),
        @('Rubinroter Kriegshelm','Episch',@('Andere','Andere','Attribute')),
        @('Fäustlinge der Ahnen','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Beinkleid der Ahnen','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Saphirumhüllte Feldhacke','Episch',@('Andere','Andere','Schaden')),
        @('Glühendes Schutzband','Episch',@('Andere','Herstellung','Schaden')),
        @('Kriegspanzer der Ahnen','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Plattenschuhe des Legionärs','Episch',@('Andere','Andere','Schaden')),
        @('Amulett der Ahnen','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Werkzeug des Goldgräbers','Selten',@('Andere','Andere'))
    )
    '3' = New-ItemEntries @(
        @('Wurzelring des Lebens','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Kristalline Schulterpanzer','Legendär',@('Andere','Andere','Schaden','Verteidigung')),
        @('Sculkwanderer Plattenschuhe','Legendär',@('Andere','Andere','Verteidigung','Herstellung')),
        @('Spitzhacke des Kaktusmeister','Legendär',@('Andere','Andere','Schaden','Attribute')),
        @('Kristallkürass','Legendär',@('Andere','Andere','Schaden','Herstellung')),
        @('Kristalliner Helmpanzer','Legendär',@('Andere','Andere','Verteidigung','Fähigkeiten')),
        @('Göttliche Pilzfällaxt','Legendär',@('Fähigkeiten','Andere','Andere','Schaden')),
        @('Kristalline Plattenhandschuhe','Legendär',@('Andere','Andere','Schaden','Attribute')),
        @('Amulett der Unsterblichkeit','Legendär',@('Andere','Andere','Fähigkeiten','Schaden')),
        @('Kristalline Plattenhose','Legendär',@('Andere','Andere','Schaden','Verteidigung')),
        @('Kristallenes Schutzband','Legendär',@('Andere','Andere','Attribute','Schaden')),
        @('Der Kaktusernter','Legendär',@('Andere','Andere','Schaden','Fähigkeiten'))
    )
    '4' = New-ItemEntries @(
        @('Göttliche Erzzerstörung','Legendär',@('Schaden','Schaden','Andere','Verteidigung')),
        @('Großer Kaktussporn','Legendär',@('Schaden','Schaden','Verteidigung','Andere')),
        @('Donnerfaust-Pilzspalteraxt','Episch',@('Andere','Andere','Herstellung')),
        @('Himmlischer Panzerhelm','Legendär',@('Andere','Andere','Fähigkeiten','Schaden')),
        @('Himmlische Plattenhandschuhe','Legendär',@('Andere','Andere','Herstellung','Fähigkeiten')),
        @('Himmlischer Gürtel','Legendär',@('Andere','Andere','Herstellung','Attribute')),
        @('Anhänger des Himmels','Legendär',@('Andere','Andere','Verteidigung','Schaden')),
        @('Himmlische Tretter','Legendär',@('Andere','Andere','Schaden','Herstellung')),
        @('Himmlische Plattenhose','Legendär',@('Andere','Andere','Verteidigung','Attribute')),
        @('Ring des Himmels','Legendär',@('Andere','Andere','Attribute','Fähigkeiten')),
        @('Himmlische Schulterplatten','Legendär',@('Andere','Andere','Schaden','Verteidigung')),
        @('Himmlischer Brustharnisch','Legendär',@('Andere','Andere','Verteidigung','Verteidigung'))
    )
}
$neu['ressourcen_kit'] = [ordered]@{
    '1' = New-ItemEntries @(
        @('Drachenschuppen Kriegshelm','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Stählernde Sabatons','Selten',@('Fähigkeiten','Attribute')),
        @('Eroberers Schulterpanzer','Selten',@('Herstellung','Fähigkeiten')),
        @('Demonischer Gürtel','Selten',@('Attribute','Fähigkeiten')),
        @('Panzerbrecher','Episch',@('Fähigkeiten','Fähigkeiten','Attribute')),
        @('Zirkonband','Ungewöhnlich',@('Fähigkeiten')),
        @('Rubinrote Kriegshandschuhe','Episch',@('Andere','Andere','Fähigkeiten')),
        @('Anhänger des Windes','Selten',@('Attribute','Fähigkeiten')),
        @('Blutrünstige Brustplatte','Selten',@('Fähigkeiten','Herstellung')),
        @('Drachenschuppen Kriegshosen','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere'))
    )
    '2' = New-ItemEntries @(
        @('Panzerbrecher','Episch',@('Fähigkeiten','Fähigkeiten','Attribute')),
        @('Dunkelmond Gamaschen','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Dunkelmond Helmvisier','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Dunkelmond Panzerhandbuckel','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Band der dunklen Herrschaft','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Dunkelmond Schulterbuckel','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Angus Abzeichen der Dunkelheit','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Sternenstahlstiefel','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Dunkelmond Brustpanzer','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Dunkelmond Gurt','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Stabiler Kornpflücker','Ungewöhnlich',@('Fähigkeiten'))
    )
    '3' = New-ItemEntries @(
        @('Der Zeitverzerrer','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Herstellung')),
        @('Mithril Sabatons','Episch',@('Fähigkeiten','Fähigkeiten','Attribute')),
        @('Zwergenstahl Plattenpanzer','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Mithril Schulterhaube','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Mithril Band','Episch',@('Fähigkeiten','Fähigkeiten','Attribute')),
        @('Zwergenstahl Plattenhandschuhe','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Majestätische Kaktushacke','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Mithril Kopfschutz','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Sternenfeuer-Anhänger','Episch',@('Fähigkeiten','Fähigkeiten','Attribute')),
        @('Mithril Gamaschen','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Sternenfeuer-Ring','Episch',@('Fähigkeiten','Fähigkeiten','Attribute'))
    )
    '4' = New-ItemEntries @(
        @('Obsidian Hacke','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Sturmgesang','Episch',@('Fähigkeiten','Fähigkeiten','Herstellung')),
        @('Nebelwacht Plattenpanzer','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Kometenband','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Der Bergversetzer','Legendär',@('Fähigkeiten','Fähigkeiten','Verteidigung','Andere')),
        @('NebelSchreiter','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Nebelwächter Gamaschen','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Nebelwächter Stulpen','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Nebelwächter Schulterhaube','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Kosmischer Anhänger','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Nebelwächter Helm','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Nebelwacht Schutzband','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Attribute')),
        @('Saphir Hacke','Selten',@('Fähigkeiten','Fähigkeiten'))
    )
}
$neu['herstellungs_kit'] = [ordered]@{
    '1' = New-ItemEntries @(
        @('Drachenschuppen Kriegshelm','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenschuppen Kriegsgürtel','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Göttliche Plattenläufer','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenschuppen Kriegshandschuhe','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Excalibur','Episch',@('Herstellung','Herstellung','Fähigkeiten')),
        @('Drachenschuppen Kriegsbrustplatte','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenschuppen Kriegsschultern','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenschuppen Kriegshosen','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenblutband','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Drachenblutamulett','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Titanstahl-Dschungelholzhacker','Episch',@('Herstellung','Attribute','Verteidigung')),
        @('Frostfänger-Eisenzerstörung','Episch',@('Herstellung','Herstellung','Fähigkeiten'))
    )
    '2' = New-ItemEntries @(
        @('Der Zeitverzerrer','Legendär',@('Fähigkeiten','Fähigkeiten','Herstellung','Herstellung')),
        @('Rubin Beil','Episch',@('Herstellung','Herstellung','Andere')),
        @('Meeres Furcher','Selten',@('Herstellung','Andere')),
        @('Sternenstahl Helmschale','Episch',@('Herstellung','Herstellung','Andere')),
        @('Sternenstahl Gürtelschutz','Episch',@('Herstellung','Herstellung','Fähigkeiten')),
        @('Band der Sterne','Episch',@('Herstellung','Herstellung','Fähigkeiten')),
        @('Sternenstahl Handrüstung','Episch',@('Herstellung','Herstellung','Andere')),
        @('Halsband der Sterne','Episch',@('Herstellung','Herstellung','Andere')),
        @('Sternenstahl Schulterrüstung','Episch',@('Herstellung','Herstellung','Andere')),
        @('Sternenstahl Leggings','Episch',@('Herstellung','Herstellung','Andere')),
        @('Sternenstahl Brustpanzer','Episch',@('Herstellung','Herstellung','Andere')),
        @('Götterstahl-Kohledezimierer','Legendär',@('Herstellung','Herstellung','Andere','Fähigkeiten'))
    )
    '3' = New-ItemEntries @(
        @('Götterstahl-Kohledezimierer','Legendär',@('Herstellung','Herstellung','Andere','Fähigkeiten')),
        @('Die Morgenröte','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Meister Spaltaxt','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Auroras Schritte','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Himmelsstahl Fingerschützer','Episch',@('Herstellung','Herstellung','Andere')),
        @('Schattenstahl Gürtelschutz','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Schattenstahl Brustpanzer','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Schattenstahl Helmpanzer','Legendär',@('Herstellung','Herstellung','Fähigkeiten','Andere')),
        @('Ring der scharfen Klinge','Episch',@('Herstellung','Herstellung','Andere')),
        @('Himmelsstahl Schulterplatten','Episch',@('Herstellung','Herstellung','Andere')),
        @('Himmelsstahl Beinschienen','Episch',@('Herstellung','Herstellung','Andere')),
        @('Teuflische Kaktushacke','Legendär',@('Herstellung','Fähigkeiten','Andere','Verteidigung')),
        @('Anhänger der Klinge','Episch',@('Herstellung','Herstellung','Andere'))
    )
}

([pscustomobject]$neu) | ConvertTo-Json -Depth 20 | Out-String | ForEach-Object {
    [System.IO.File]::WriteAllText($outPath, $_, [System.Text.UTF8Encoding]::new($false))
}
Write-Host "Wrote $outPath"
